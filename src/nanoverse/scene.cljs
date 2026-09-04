(ns nanoverse.scene
  (:require [nanoverse.vec3 :as v]
            [nanoverse.deck :as deck]
            [nanoverse.solvent :as sol]))

;; The Babylon half of a "functional group in water" slide: everything that is
;; the same whichever group is on screen. All the chemistry lives in
;; nanoverse.solvent and in each slide's own geometry namespace; nothing here
;; knows what a pKa is.
;;
;; A slide hands over one or more *states* (a protonation state, a redox
;; state) and gets back a handle per state. Every state is built once, up
;; front, and switching between them is a visibility flip -- so dragging a pH
;; slider back and forth never rebuilds a mesh.

(def UNITS-PER-A 0.55) ; Babylon scene units per angstrom

(defn to-vec3 [a]
  (js/BABYLON.Vector3. (* (:x a) UNITS-PER-A) (* (:y a) UNITS-PER-A) (* (:z a) UNITS-PER-A)))

;; Drawn radii. Not van der Waals radii -- ball-and-stick spheres that size
;; would swallow the bonds -- but ordered like them, so sulfur and phosphorus
;; read as the big atoms they are next to oxygen and nitrogen.
(def R {:C 0.32 :O 0.34 :N 0.33 :S 0.44 :P 0.44 :SE 0.48 :H 0.20})

;; CPK-ish, matched to the palette the deck already uses for C/O/H. Selenium
;; is drawn as a heavier, browner sulfur, which is roughly what it is.
(def ELEMENT-COLOR
  {:C "#4b4f58" :O "#e2555a" :N "#5b7fd4" :S "#e3c04a" :P "#e8894a"
   :SE "#b8862b" :H "#eef1f4"})

(def BOND-RADIUS 0.045)
(def HBOND-RADIUS 0.035)
(def DOUBLE-GAP 0.135)      ; angstroms, half-separation of a double bond's tubes
(def DOUBLE-RADIUS 0.032)

;; ---------------------------------------------------------------------
;; Scene setup
;; ---------------------------------------------------------------------

(defn init
  "Engine, scene, camera, lights and the shared material set for one slide."
  [prefix {:keys [alpha beta radius lower upper]
           :or {alpha (- (/ js/Math.PI 2) 0.6) beta 1.15 radius 9.0 lower 4 upper 24}}]
  (let [canvas (deck/el prefix "canvas")
        engine (js/BABYLON.Engine. canvas true)
        scene (js/BABYLON.Scene. engine)
        [r g b] (deck/hex->rgb01 "#0d1219")
        camera (js/BABYLON.ArcRotateCamera. "cam" alpha beta radius
                                            (js/BABYLON.Vector3. 0 0 0) scene)
        hemi (js/BABYLON.HemisphericLight. "hemi" (js/BABYLON.Vector3. 0 1 0.2) scene)
        dir (js/BABYLON.DirectionalLight. "dir" (js/BABYLON.Vector3. -0.5 -1 -0.3) scene)
        glow (js/BABYLON.GlowLayer. "glow" scene)
        mat (fn [hex glow?] (deck/make-material scene hex glow?))
        charge-mat (fn [hex]
                     (let [m (deck/make-material scene hex false)]
                       (set! (.-alpha m) 0.22)
                       ;; front faces only: drawing both hemispheres stacks two
                       ;; translucent layers and the "cloud" turns into a muddy
                       ;; ball that hides the atom inside it
                       (set! (.-backFaceCulling m) true)
                       m))]
    (set! (.-clearColor scene) (js/BABYLON.Color4. r g b 1))
    (set! (.-lowerRadiusLimit camera) lower)
    (set! (.-upperRadiusLimit camera) upper)
    (set! (.-wheelPrecision camera) 40)
    (set! (.-intensity hemi) 0.75)
    (set! (.-intensity dir) 0.55)
    (set! (.-intensity glow) 0.7)
    {:canvas canvas :engine engine :scene scene :camera camera
     :materials (merge (into {} (for [[e hex] ELEMENT-COLOR] [e (mat hex false)]))
                       {:bond (mat "#9aa5b1" false)
                        ;; only the H-bond materials get an emissiveColor, so
                        ;; GlowLayer lights those and nothing else
                        :hbond (mat "#59e0c9" true)
                        :hbond-weak (mat "#2f7d70" true)
                        :bent (mat "#6d4b52" false)
                        :phobic (mat "#7a6a4a" false)
                        :neg (charge-mat "#e2555a")
                        :pos (charge-mat "#5b7fd4")})}))

;; ---------------------------------------------------------------------
;; Drawing a molecule
;; ---------------------------------------------------------------------

(defn- bond-plane-normal
  "A direction perpendicular to the a-b bond, chosen inside the local plane so
   a double bond's two tubes lie where the sp2 plane actually is."
  [atoms bonds a b]
  (let [axis (v/norm (v/sub (get atoms b) (get atoms a)))
        third (first (filter #(and (not= % b) (sol/heavy? atoms %))
                             (concat (sol/neighbours bonds a) (sol/neighbours bonds b))))
        ref (if third (v/sub (get atoms third) (get atoms a)) {:x 0 :y 0 :z 1})
        n (v/cross axis ref)]
    (if (< (v/len n) 1e-6)
      (v/norm (v/cross axis {:x 1 :y 0 :z 0}))
      (v/norm (v/cross axis (v/norm n))))))

(defn molecule!
  "Every atom and bond of one state, parented to a single node. Returns the
   node plus the per-atom meshes, so a slide can recolour a group later."
  [ctx {:keys [atoms bonds charges]}]
  (let [{:keys [scene materials]} ctx
        root (js/BABYLON.TransformNode. "molecule" scene)
        atom-meshes
        (into {}
              (for [k (keys atoms)]
                (let [e (:e (get atoms k))
                      s (deck/make-sphere scene (str "atom-" (name k)) (get R e 0.3)
                                          (get materials e) (to-vec3 (get atoms k)))]
                  (set! (.-parent s) root)
                  [k s])))]
    (doseq [{:keys [a b order]} bonds]
      (let [pa (get atoms a) pb (get atoms b)]
        (if (= order 1)
          (let [t (deck/make-tube scene (str "bond-" (name a) "-" (name b))
                                  (to-vec3 pa) (to-vec3 pb) BOND-RADIUS (:bond materials))]
            (set! (.-parent t) root))
          ;; order 2 draws two equal tubes; order 1.5 draws a full tube with a
          ;; thinner partner alongside, for a bond the coordinates say is
          ;; delocalised rather than double
          (let [off (v/scale (bond-plane-normal atoms bonds a b) DOUBLE-GAP)
                halves (if (= order 2) [[1 DOUBLE-RADIUS] [-1 DOUBLE-RADIUS]]
                                       [[0 BOND-RADIUS] [1 DOUBLE-RADIUS]])]
            (doseq [[s rad] halves]
              (let [d (v/scale off s)
                    t (deck/make-tube scene (str "bond-" (name a) "-" (name b) "-" s)
                                      (to-vec3 (v/add pa d)) (to-vec3 (v/add pb d))
                                      rad (:bond materials))]
                (set! (.-parent t) root)))))))
    ;; formal charge, drawn as a translucent shell around the atom carrying it
    (doseq [k (keys (or charges {}))]
      (let [q (get charges k)
            s (deck/make-sphere scene (str "charge-" (name k))
                                (* (get R (:e (get atoms k)) 0.3) 2.6)
                                (if (neg? q) (:neg materials) (:pos materials))
                                (to-vec3 (get atoms k)))]
        (set! (.-parent s) root)))
    {:root root :atom-meshes atom-meshes}))

;; ---------------------------------------------------------------------
;; A whole state: molecule + its waters + one tube per candidate H-bond
;; ---------------------------------------------------------------------

(defn state-group! [ctx state]
  (let [{:keys [scene materials]} ctx
        mol (molecule! ctx state)
        waters (:waters state)
        pairs (sol/candidate-pairs state)
        water-nodes
        (mapv (fn [w]
                (let [node (js/BABYLON.TransformNode. (str "water-" (:name w)) scene)
                      local (:local w)
                      o-pos (to-vec3 (:O local))
                      h1 (to-vec3 (:H1 local))
                      h2 (to-vec3 (:H2 local))]
                  (set! (.-position node) (to-vec3 (:base-o w)))
                  (set! (.-rotationQuaternion node) (js/BABYLON.Quaternion. 0 0 0 1))
                  (doseq [m [(deck/make-sphere scene (str (:name w) "-O") (:O R) (:O materials) o-pos)
                             (deck/make-sphere scene (str (:name w) "-H1") (:H R) (:H materials) h1)
                             (deck/make-sphere scene (str (:name w) "-H2") (:H R) (:H materials) h2)
                             (deck/make-tube scene (str (:name w) "-b1") o-pos h1 BOND-RADIUS (:bond materials))
                             (deck/make-tube scene (str (:name w) "-b2") o-pos h2 BOND-RADIUS (:bond materials))]]
                    (set! (.-parent m) node))
                  node))
              waters)
        ;; material varies per frame (bonded / weak / rejected), so the tubes
        ;; are created without one
        tubes (mapv (fn [_] (deck/make-hbond-tube scene HBOND-RADIUS nil)) pairs)]
    {:state state :mol mol :water-nodes water-nodes :tubes tubes :pairs pairs :visible (atom true)}))

(defn show!
  "Switch a whole state on or off. Disabling the parent node takes its atoms,
   bonds and waters with it; the H-bond tubes are top-level meshes and have to
   be told separately."
  [group on?]
  (reset! (:visible group) on?)
  (.setEnabled (:root (:mol group)) on?)
  (doseq [n (:water-nodes group)] (.setEnabled n on?))
  (doseq [t (:tubes group)]
    (when-not on? (set! (.-isVisible (:mesh t)) false))
    (.setEnabled (:mesh t) on?)))

(defn update!
  "Advance one state's waters to time t, refresh its H-bond tubes, and return
   the evaluated candidate pairs. Counting them is left to the slide, which
   knows which sites it wants to talk about."
  [ctx group {:keys [t amp tumble show-bent min-strength]
              :or {tumble 0 show-bent false min-strength 0}}]
  (let [{:keys [materials]} ctx
        {:keys [state water-nodes tubes pairs]} group
        atoms (:atoms state)
        current (mapv #(sol/compute-current % t amp tumble) (:waters state))]
    (dotimes [i (count water-nodes)]
      (let [node (nth water-nodes i)
            c (nth current i)
            q (:quat c)]
        (.copyFrom (.-position node) (to-vec3 (:O c)))
        (.copyFromFloats (.-rotationQuaternion node) (:x q) (:y q) (:z q) (:w q))))
    (mapv (fn [idx]
            (let [r (sol/evaluate (nth pairs idx) atoms current)
                  strong? (>= (:strength r) min-strength)
                  lit? (and (:active r) strong?)
                  draw? (or lit? (and (:bent r) show-bent))
                  {:keys [mesh opts]} (nth tubes idx)]
              (set! (.-isVisible mesh) draw?)
              (when draw?
                (set! (.-material mesh)
                      (cond lit? (if (< (:strength r) 12) (:hbond-weak materials) (:hbond materials))
                            :else (:bent materials)))
                (.copyFrom (aget (.-path opts) 0) (to-vec3 (:H r)))
                (.copyFrom (aget (.-path opts) 1) (to-vec3 (:A r)))
                (set! (.-instance opts) mesh)
                (js/BABYLON.MeshBuilder.CreateTube "hbond" opts))
              (assoc r :lit lit?)))
          (range (count pairs)))))

;; ---------------------------------------------------------------------
;; The controls every slide shares
;; ---------------------------------------------------------------------

(defn- wire-slider! [prefix id st k fmt]
  (when-let [input (deck/el prefix id)]
    (let [out (deck/el prefix (str id "-value"))]
      (swap! st assoc k (js/parseFloat (.-value input)))
      (.addEventListener input "input"
        (fn []
          (let [x (js/parseFloat (.-value input))]
            (swap! st assoc k x)
            (when out (set! (.-textContent out) (fmt x)))))))))

(defn controls!
  "Wire whichever of the standard controls this slide actually has in the DOM
   -- a slide with no tumble knob simply omits the element."
  [prefix]
  (let [st (atom {:amplitude 0.3 :tumble 0 :speed 1
                  :running (not deck/reduce-motion?)
                  :t-accum 0 :last-frame nil})
        play (deck/el prefix "play")
        bent (deck/el prefix "show-bent")]
    (wire-slider! prefix "amp" st :amplitude (fn [x] (str (.toFixed x 2) " Å")))
    (wire-slider! prefix "tumble" st :tumble (fn [x] (str (.toFixed x 0) "°")))
    (wire-slider! prefix "speed" st :speed (fn [x] (str (.toFixed x 1) "×")))
    (when bent
      (swap! st assoc :show-bent (.-checked bent))
      (.addEventListener bent "change" (fn [] (swap! st assoc :show-bent (.-checked bent)))))
    (when play
      (set! (.-textContent play) (if (:running @st) "Pause" "Play"))
      (.addEventListener play "click"
        (fn []
          (swap! st update :running not)
          (set! (.-textContent play) (if (:running @st) "Pause" "Play")))))
    st))

(defn titration!
  "Wire a pH slider to an ordered ladder of protonation states. `groups` is
   indexed by how many protons have been given up and `pkas` is the ascending
   pKa ladder between them; the group drawn is the majority species at the
   current pH.

   `on-ph` is called with [pH step] whenever the slider moves, for the
   readouts that depend on pH but not on the frame. Returns an atom holding
   the visible group, which the render loop reads each frame."
  [prefix st groups pkas on-ph]
  (let [input (deck/el prefix "ph")
        out (deck/el prefix "ph-value")
        step (atom -1)
        current (atom (nth groups 0))]
    (letfn [(apply-ph! [ph]
              (let [n (sol/majority-step ph pkas)]
                (swap! st assoc :ph ph)
                (when out (set! (.-textContent out) (.toFixed ph 1)))
                (when-not (= n @step)
                  (dotimes [i (count groups)]
                    (show! (nth groups i) (= i n)))
                  (reset! step n)
                  (reset! current (nth groups n)))
                (when on-ph (on-ph ph n))))]
      (if input
        (do (apply-ph! (js/parseFloat (.-value input)))
            (.addEventListener input "input"
              (fn [] (apply-ph! (js/parseFloat (.-value input))))))
        (apply-ph! 7.0))
      current)))

(defn tick!
  "Advance the slide clock and return the new time. Capped per frame so a
   slide that was hidden for a minute does not resume with a huge jump."
  [st]
  (let [{:keys [running speed last-frame t-accum]} @st
        now (js/performance.now)
        dt (js/Math.min (/ (- now (or last-frame now)) 1000) 0.05)
        t' (if running (+ t-accum (* dt speed)) t-accum)]
    (swap! st assoc :last-frame now :t-accum t')
    t'))

;; ---------------------------------------------------------------------
;; Readout helpers
;; ---------------------------------------------------------------------

(defn set-text! [prefix id s]
  (when-let [e (deck/el prefix id)]
    (set! (.-textContent e) (str s))))

(defn set-num!
  "Write a readout and grey it out when it is zero -- a zero that means
   'never, by construction' should look different from a live count."
  [prefix id n]
  (when-let [e (deck/el prefix id)]
    (set! (.-textContent e) (str n))
    (set! (.-className e) (str "num" (if (zero? n) " zero" "")))))

(defn count-lit
  ([rs] (count (filter :lit rs)))
  ([rs site] (count (filter #(and (:lit %) (= (:site %) site)) rs))))

(def signed sol/signed-charge)
