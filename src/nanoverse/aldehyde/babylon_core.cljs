(ns nanoverse.aldehyde.babylon-core
  (:require [nanoverse.vec3 :as v]
            [nanoverse.deck :as deck]
            [nanoverse.aldehyde.geometry :as geo]))

;; Babylon.js rendering for the aldehyde slide. All chemistry lives in
;; geometry.cljs; this file only builds meshes and drives the frame loop.
;; Everything hangs off `build` so the deck can mount it on demand.

(def UNITS-PER-A 0.55) ; Babylon scene units per angstrom

(defn to-vec3 [a]
  (js/BABYLON.Vector3. (* (:x a) UNITS-PER-A) (* (:y a) UNITS-PER-A) (* (:z a) UNITS-PER-A)))

(defn element-of [k]
  ;; :C1 -> :C, :H2a -> :H, :O1 -> :O
  (keyword (subs (name k) 0 1)))

(def R {:C 0.32 :O 0.34 :H 0.2})
(def BOND-RADIUS 0.045)
(def HBOND-RADIUS 0.035)
(def DOUBLE-BOND-GAP 0.135)    ; angstroms, half-separation of the two C=O tubes
(def DOUBLE-BOND-RADIUS 0.032) ; thinner than a single bond, so the pair reads as two lines

(defn build [prefix]
  (let [el #(deck/el prefix %)
        canvas (el "canvas")
        engine (js/BABYLON.Engine. canvas true)
        scene (js/BABYLON.Scene. engine)

        _ (let [[r g b] (deck/hex->rgb01 "#0d1219")]
            (set! (.-clearColor scene) (js/BABYLON.Color4. r g b 1)))

        camera (js/BABYLON.ArcRotateCamera. "cam" (- (/ js/Math.PI 2) 0.7) 1.2 9.0
                                            (js/BABYLON.Vector3. 0 0 0) scene)
        _ (do (set! (.-lowerRadiusLimit camera) 4)
              (set! (.-upperRadiusLimit camera) 22)
              (set! (.-wheelPrecision camera) 40))

        hemi (js/BABYLON.HemisphericLight. "hemi" (js/BABYLON.Vector3. 0 1 0.2) scene)
        _ (set! (.-intensity hemi) 0.75)
        dir (js/BABYLON.DirectionalLight. "dir" (js/BABYLON.Vector3. -0.5 -1 -0.3) scene)
        _ (set! (.-intensity dir) 0.55)

        glow (js/BABYLON.GlowLayer. "glow" scene)
        _ (set! (.-intensity glow) 0.7)

        ;; Only the real hydrogen-bond material gets an emissiveColor, so
        ;; GlowLayer lights those and nothing else -- the rejected "bent"
        ;; contacts stay deliberately dull.
        mat (fn [hex glow?] (deck/make-material scene hex glow?))
        materials {:C (mat "#4b4f58" false)
                   :O (mat "#e2555a" false)
                   :H (mat "#eef1f4" false)
                   :bond (mat "#9aa5b1" false)
                   :carbonyl (mat "#59e0c9" true)   ; H-bond at the C=O -- the only kind here
                   :bent (mat "#6d4b52" false)}     ; close enough, wrong angle

        sphere (fn [nm d material pos] (deck/make-sphere scene nm d material pos))
        tube (fn [nm a b radius material] (deck/make-tube scene nm a b radius material))

        ;; the molecule (static)
        mol-group (js/BABYLON.TransformNode. "acetaldehyde" scene)
        _ (doseq [k geo/mol-keys]
            (let [e (element-of k)
                  s (sphere (str "mol-" (name k)) (get R e) (get materials e)
                            (to-vec3 (get geo/mol k)))]
              (set! (.-parent s) mol-group)))

        ;; A double bond is drawn as two parallel tubes offset within the sp2
        ;; plane. The order comes from the SDF, not from a hand-picked list.
        _ (doseq [{:keys [a b order]} geo/mol-bonds]
            (let [pa (get geo/mol a)
                  pb (get geo/mol b)]
              (if (= order 2)
                (let [axis (v/norm (v/sub pb pa))
                      off (v/scale (v/norm (v/cross axis geo/carbonyl-normal)) DOUBLE-BOND-GAP)]
                  (doseq [s [1 -1]]
                    (let [d (v/scale off s)
                          t (tube (str "mol-bond-" (name a) "-" (name b) "-" s)
                                  (to-vec3 (v/add pa d)) (to-vec3 (v/add pb d))
                                  DOUBLE-BOND-RADIUS (:bond materials))]
                      (set! (.-parent t) mol-group))))
                (let [t (tube (str "mol-bond-" (name a) "-" (name b))
                              (to-vec3 pa) (to-vec3 pb) BOND-RADIUS (:bond materials))]
                  (set! (.-parent t) mol-group)))))

        ;; waters: one TransformNode each, moved AND rotated per frame, so each
        ;; water tumbles as one rigid body
        anchors (mapv (fn [w]
                        (let [node (js/BABYLON.TransformNode. (str "water-" (:name w)) scene)
                              local (:local w)
                              o-pos (to-vec3 (:O local))
                              h1-pos (to-vec3 (:H1 local))
                              h2-pos (to-vec3 (:H2 local))]
                          (set! (.-position node) (to-vec3 (:base-o w)))
                          (set! (.-rotationQuaternion node) (js/BABYLON.Quaternion. 0 0 0 1))
                          (doseq [m [(sphere (str (:name w) "-O") (:O R) (:O materials) o-pos)
                                     (sphere (str (:name w) "-H1") (:H R) (:H materials) h1-pos)
                                     (sphere (str (:name w) "-H2") (:H R) (:H materials) h2-pos)
                                     (tube (str (:name w) "-b1") o-pos h1-pos BOND-RADIUS (:bond materials))
                                     (tube (str (:name w) "-b2") o-pos h2-pos BOND-RADIUS (:bond materials))]]
                            (set! (.-parent m) node))
                          node))
                      geo/waters)

        ;; material varies per frame (bonded vs rejected), so these are
        ;; created without one
        tubes (mapv (fn [_] (deck/make-hbond-tube scene HBOND-RADIUS nil)) geo/pairs)

        amp-input (el "amp")
        tumble-input (el "tumble")
        speed-input (el "speed")
        amp-value-el (el "amp-value")
        tumble-value-el (el "tumble-value")
        speed-value-el (el "speed-value")
        play-toggle (el "play")
        show-bent-input (el "show-bent")
        readout-count (el "count")
        readout-gives (el "gives")
        readout-takes (el "takes")
        readout-bent (el "bent")

        state (atom {:amplitude (js/parseFloat (.-value amp-input))
                     :tumble (js/parseFloat (.-value tumble-input))
                     :speed (js/parseFloat (.-value speed-input))
                     :show-bent (.-checked show-bent-input)
                     :running (not deck/reduce-motion?)
                     :t-accum 0
                     :last-frame nil})]

    (set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))

    (.addEventListener amp-input "input"
      (fn []
        (let [x (js/parseFloat (.-value amp-input))]
          (swap! state assoc :amplitude x)
          (set! (.-textContent amp-value-el) (str (.toFixed x 2) " Å")))))

    (.addEventListener tumble-input "input"
      (fn []
        (let [x (js/parseFloat (.-value tumble-input))]
          (swap! state assoc :tumble x)
          (set! (.-textContent tumble-value-el) (str (.toFixed x 0) "°")))))

    (.addEventListener speed-input "input"
      (fn []
        (let [x (js/parseFloat (.-value speed-input))]
          (swap! state assoc :speed x)
          (set! (.-textContent speed-value-el) (str (.toFixed x 1) "×")))))

    (.addEventListener show-bent-input "change"
      (fn [] (swap! state assoc :show-bent (.-checked show-bent-input))))

    (.addEventListener play-toggle "click"
      (fn []
        (swap! state update :running not)
        (set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))))

    (letfn [(frame []
              (let [{:keys [running amplitude tumble speed last-frame t-accum show-bent]} @state
                    now (js/performance.now)
                    lf (or last-frame now)
                    dt (js/Math.min (/ (- now lf) 1000) 0.05)
                    t' (if running (+ t-accum (* dt speed)) t-accum)
                    current (mapv #(geo/compute-current % t' amplitude tumble) geo/waters)
                    n-active (atom 0)
                    ;; kept as a live count rather than a hardcoded 0 so the
                    ;; readout is measuring the same thing the hydroxyl slide
                    ;; measures -- it stays at zero because no candidate pair
                    ;; can put this molecule on the donating side, not because
                    ;; anything special-cases it here
                    n-gives (atom 0)
                    n-takes (atom 0)
                    n-bent (atom 0)]
                (swap! state assoc :last-frame now :t-accum t')

                (dotimes [i (count geo/waters)]
                  (let [node (nth anchors i)
                        c (nth current i)
                        q (:quat c)]
                    (.copyFrom (.-position node) (to-vec3 (:O c)))
                    (.copyFromFloats (.-rotationQuaternion node) (:x q) (:y q) (:z q) (:w q))))

                (dotimes [idx (count geo/pairs)]
                  (let [r (geo/evaluate (nth geo/pairs idx) current)
                        {:keys [mesh opts]} (nth tubes idx)
                        draw? (or (:active r) (and (:bent r) show-bent))]
                    (set! (.-isVisible mesh) draw?)
                    (when draw?
                      (set! (.-material mesh)
                            (if (:active r) (:carbonyl materials) (:bent materials)))
                      (.copyFrom (aget (.-path opts) 0) (to-vec3 (:H r)))
                      (.copyFrom (aget (.-path opts) 1) (to-vec3 (:A r)))
                      (set! (.-instance opts) mesh)
                      (js/BABYLON.MeshBuilder.CreateTube "hbond" opts))
                    (when (:active r)
                      (swap! n-active inc)
                      (if (= (:kind r) :molecule-donates)
                        (swap! n-gives inc)
                        (swap! n-takes inc)))
                    (when (:bent r) (swap! n-bent inc))))

                (set! (.-textContent readout-count) @n-active)
                (set! (.-textContent readout-gives) @n-gives)
                (set! (.-textContent readout-takes) @n-takes)
                (set! (.-textContent readout-bent) @n-bent)
                (set! (.-className readout-gives)
                      (str "num" (if (zero? @n-gives) " zero" "")))))]

      (deck/slide-handle engine scene camera canvas frame))))
