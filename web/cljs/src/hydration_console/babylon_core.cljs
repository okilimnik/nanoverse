(ns hydration-console.babylon-core
  (:require [hydration-console.geometry :as geo]))

;; Babylon.js port of core.cljs (the Zdog version) -- same chemistry, same
;; wobble, same hydrogen-bond distance/angle test (all in geometry.cljs,
;; untouched). Only the rendering engine changes: real WebGL/3D via
;; Babylon instead of Zdog's flat pseudo-3D.

(def UNITS-PER-A 0.55) ; Babylon scene units per angstrom (arbitrary but consistent)

(defn to-vec3 [a]
  (js/BABYLON.Vector3. (* (:x a) UNITS-PER-A) (* (:y a) UNITS-PER-A) (* (:z a) UNITS-PER-A)))

(defn element-of [k] (keyword (subs (name k) 0 1))) ; :C1 -> :C, :H4 -> :H, :O -> :O

(defn hex->rgb01 [hex]
  (let [h (subs hex 1)]
    [(/ (js/parseInt (subs h 0 2) 16) 255)
     (/ (js/parseInt (subs h 2 4) 16) 255)
     (/ (js/parseInt (subs h 4 6) 16) 255)]))

;; ---------------------------------------------------------------------
;; scene / camera / lights
;; ---------------------------------------------------------------------
(def canvas (js/document.getElementById "render-canvas"))
(def engine (js/BABYLON.Engine. canvas true))
(def scene (js/BABYLON.Scene. engine))

(let [[r g b] (hex->rgb01 "#0d1219")]
  (set! (.-clearColor scene) (js/BABYLON.Color4. r g b 1)))

(def camera
  (js/BABYLON.ArcRotateCamera. "cam" (- (/ js/Math.PI 2) 0.5) 1.15 8.5
                                (js/BABYLON.Vector3. 0 0 0)
                                scene))
(.attachControl camera canvas true)
(set! (.-lowerRadiusLimit camera) 4)
(set! (.-upperRadiusLimit camera) 20)
(set! (.-wheelPrecision camera) 40)

(def hemi-light (js/BABYLON.HemisphericLight. "hemi" (js/BABYLON.Vector3. 0 1 0.2) scene))
(set! (.-intensity hemi-light) 0.75)
(def dir-light (js/BABYLON.DirectionalLight. "dir" (js/BABYLON.Vector3. -0.5 -1 -0.3) scene))
(set! (.-intensity dir-light) 0.55)

(def glow (js/BABYLON.GlowLayer. "glow" scene))
(set! (.-intensity glow) 0.7)

;; ---------------------------------------------------------------------
;; materials (shared, one per color -- only hbond gets emissiveColor, so
;; GlowLayer only lights up the H-bond tubes, no exclusion lists needed)
;; ---------------------------------------------------------------------
(defn make-material [hex glow?]
  (let [[r g b] (hex->rgb01 hex)
        m (js/BABYLON.StandardMaterial. (str "mat-" hex) scene)
        c (js/BABYLON.Color3. r g b)]
    (set! (.-diffuseColor m) c)
    (when glow? (set! (.-emissiveColor m) c))
    m))

(def materials
  {:C (make-material "#4b4f58" false)
   :O (make-material "#e2555a" false)
   :H (make-material "#eef1f4" false)
   :bond (make-material "#9aa5b1" false)
   :hbond (make-material "#59e0c9" true)})

(def R {:C 0.32 :O 0.34 :H 0.2})
(def BOND-RADIUS 0.045)
(def HBOND-RADIUS 0.035)

(defn make-sphere [name-str diameter material pos]
  (let [s (js/BABYLON.MeshBuilder.CreateSphere name-str #js {:diameter diameter :segments 20} scene)]
    (set! (.-position s) pos)
    (set! (.-material s) material)
    s))

(defn make-bond [name-str a-pos b-pos]
  (let [t (js/BABYLON.MeshBuilder.CreateTube name-str
            #js {:path #js [a-pos b-pos] :radius BOND-RADIUS :tessellation 8}
            scene)]
    (set! (.-material t) (:bond materials))
    t))

;; ethanol, static
(def eth-group (js/BABYLON.TransformNode. "ethanol" scene))
(doseq [k geo/eth-keys]
  (let [el (element-of k)
        s (make-sphere (str "eth-" (name k)) (get R el) (get materials el) (to-vec3 (get geo/eth k)))]
    (set! (.-parent s) eth-group)))
(doseq [[a b] geo/eth-bonds]
  (let [t (make-bond (str "eth-bond-" (name a) "-" (name b)) (to-vec3 (get geo/eth a)) (to-vec3 (get geo/eth b)))]
    (set! (.-parent t) eth-group)))

;; waters: one TransformNode per water, translated each frame; local
;; shapes (fixed relative to that node) built once. Index-aligned with
;; geo/waters.
(def anchors
  (mapv (fn [w]
          (let [node (js/BABYLON.TransformNode. (str "water-" (:name w)) scene)
                local (:local w)
                o-pos (to-vec3 (:O local))
                h1-pos (to-vec3 (:H1 local))
                h2-pos (to-vec3 (:H2 local))]
            (set! (.-position node) (to-vec3 (:base-o w)))
            (doseq [m [(make-sphere (str (:name w) "-O") (:O R) (:O materials) o-pos)
                       (make-sphere (str (:name w) "-H1") (:H R) (:H materials) h1-pos)
                       (make-sphere (str (:name w) "-H2") (:H R) (:H materials) h2-pos)
                       (make-bond (str (:name w) "-b1") o-pos h1-pos)
                       (make-bond (str (:name w) "-b2") o-pos h2-pos)]]
              (set! (.-parent m) node))
            node))
        geo/waters))

;; H-bond candidate tubes, index-aligned with geo/pairs. Each keeps its
;; own mutable `opts` object around -- Babylon's documented cheap-update
;; pattern mutates the existing path Vector3s in place, sets
;; opts.instance, and calls CreateTube again with the SAME opts object.
(def tubes
  (mapv (fn [_]
          (let [opts #js {:path #js [(js/BABYLON.Vector3. 0 0 0) (js/BABYLON.Vector3. 0 0 0.001)]
                           :radius HBOND-RADIUS :tessellation 8 :updatable true}
                mesh (js/BABYLON.MeshBuilder.CreateTube "hbond" opts scene)]
            (set! (.-material mesh) (:hbond materials))
            (set! (.-isVisible mesh) false)
            {:mesh mesh :opts opts}))
        geo/pairs))

;; ---------------------------------------------------------------------
;; controls
;; ---------------------------------------------------------------------
(def amp-input (js/document.getElementById "amp"))
(def speed-input (js/document.getElementById "speed"))
(def amp-value-el (js/document.getElementById "amp-value"))
(def speed-value-el (js/document.getElementById "speed-value"))
(def play-toggle (js/document.getElementById "play-toggle"))
(def readout-count (js/document.getElementById "readout-count"))
(def readout-dist (js/document.getElementById "readout-dist"))

(def reduce-motion?
  (and js/window.matchMedia
       (.-matches (js/window.matchMedia "(prefers-reduced-motion: reduce)"))))

;; one state atom, same shape as the Zdog version
(def state
  (atom {:amplitude (js/parseFloat (.-value amp-input))
         :speed (js/parseFloat (.-value speed-input))
         :running (not reduce-motion?)
         :t-accum 0
         :last-frame nil}))

(set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))

(.addEventListener amp-input "input"
  (fn []
    (let [v (js/parseFloat (.-value amp-input))]
      (swap! state assoc :amplitude v)
      (set! (.-textContent amp-value-el) (str (.toFixed v 2) " Å")))))

(.addEventListener speed-input "input"
  (fn []
    (let [v (js/parseFloat (.-value speed-input))]
      (swap! state assoc :speed v)
      (set! (.-textContent speed-value-el) (str (.toFixed v 1) "×")))))

(.addEventListener play-toggle "click"
  (fn []
    (swap! state update :running not)
    (set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))))

(js/window.addEventListener "resize" (fn [] (.resize engine)))

;; ---------------------------------------------------------------------
;; animation loop -- driven by engine.runRenderLoop, not our own
;; requestAnimationFrame; identical control flow to the Zdog version's
;; frame function otherwise.
;; ---------------------------------------------------------------------
(defn frame []
  (let [{:keys [running amplitude speed last-frame t-accum]} @state
        now (js/performance.now)
        lf (or last-frame now)
        dt (js/Math.min (/ (- now lf) 1000) 0.05)
        t-accum' (if running (+ t-accum (* dt speed)) t-accum)
        current (mapv #(geo/compute-current % t-accum' amplitude) geo/waters)
        active-count (atom 0)
        nearest (atom js/Infinity)]
    (swap! state assoc :last-frame now :t-accum t-accum')

    (dotimes [i (count geo/waters)]
      (.copyFrom (.-position (nth anchors i)) (to-vec3 (:O (nth current i)))))

    (dotimes [idx (count geo/pairs)]
      (let [p (nth geo/pairs idx)
            has-donor (some? (:donor p))
            w-idx (if has-donor (:donor p) (:acceptor p))
            c (nth current w-idx)
            H (if has-donor
                (if (= (:donor-atom p) :H1) (:H1 c) (:H2 c))
                (:H9 geo/eth))
            D (if has-donor (:O c) (:O geo/eth))
            A (if has-donor (:O geo/eth) (:O c))
            dist-vec (geo/sub A H)
            dist (geo/vlen dist-vec)
            vec-hd (geo/vnorm (geo/sub D H))
            vec-ha (geo/vnorm dist-vec)
            cos-angle (geo/vdot vec-hd vec-ha)
            active (and (< dist geo/DIST-LIMIT) (< cos-angle geo/COS-LIMIT))
            {:keys [mesh opts]} (nth tubes idx)]
        (set! (.-isVisible mesh) active)
        (when active
          (.copyFrom (aget (.-path opts) 0) (to-vec3 H))
          (.copyFrom (aget (.-path opts) 1) (to-vec3 A))
          (set! (.-instance opts) mesh)
          (js/BABYLON.MeshBuilder.CreateTube "hbond" opts)
          (swap! active-count inc))
        (when (< dist @nearest) (reset! nearest dist))))

    (set! (.-textContent readout-count) @active-count)
    (set! (.-className readout-count) (str "num" (if (zero? @active-count) " zero" "")))
    (set! (.-textContent readout-dist)
          (if (js/isFinite @nearest) (str (.toFixed @nearest 2) " Å") "—"))))

(.runRenderLoop engine (fn [] (frame) (.render scene)))
