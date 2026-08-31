(ns nanoverse.hydroxyl.babylon-core
  (:require [nanoverse.vec3 :as v]
            [nanoverse.deck :as deck]
            [nanoverse.hydroxyl.geometry :as geo]))

;; Babylon.js rendering for the hydroxyl (ethanol) slide. All chemistry lives in
;; geometry.cljs; this file only builds meshes and drives the frame loop.
;;
;; Everything hangs off `build`, which the deck calls once with this slide's
;; DOM id prefix. Nothing runs at module load, so several slides can coexist
;; on one page without fighting over element ids or starting stray render
;; loops.

(def UNITS-PER-A 0.55) ; Babylon scene units per angstrom (arbitrary but consistent)

(defn to-vec3 [a]
  (js/BABYLON.Vector3. (* (:x a) UNITS-PER-A) (* (:y a) UNITS-PER-A) (* (:z a) UNITS-PER-A)))

(defn element-of [k] (keyword (subs (name k) 0 1))) ; :C1 -> :C, :H4 -> :H, :O -> :O

(defn build [prefix]
  (let [el #(deck/el prefix %)
        canvas (el "canvas")
        engine (js/BABYLON.Engine. canvas true)
        scene (js/BABYLON.Scene. engine)

        _ (let [[r g b] (deck/hex->rgb01 "#0d1219")]
            (set! (.-clearColor scene) (js/BABYLON.Color4. r g b 1)))

        camera (js/BABYLON.ArcRotateCamera. "cam" (- (/ js/Math.PI 2) 0.5) 1.15 8.5
                                            (js/BABYLON.Vector3. 0 0 0) scene)
        _ (do (set! (.-lowerRadiusLimit camera) 4)
              (set! (.-upperRadiusLimit camera) 20)
              (set! (.-wheelPrecision camera) 40))

        hemi (js/BABYLON.HemisphericLight. "hemi" (js/BABYLON.Vector3. 0 1 0.2) scene)
        _ (set! (.-intensity hemi) 0.75)
        dir (js/BABYLON.DirectionalLight. "dir" (js/BABYLON.Vector3. -0.5 -1 -0.3) scene)
        _ (set! (.-intensity dir) 0.55)

        glow (js/BABYLON.GlowLayer. "glow" scene)
        _ (set! (.-intensity glow) 0.7)

        ;; materials (shared, one per color -- only hbond gets emissiveColor, so
        ;; GlowLayer only lights up the H-bond tubes, no exclusion lists needed)
        mat (fn [hex glow?] (deck/make-material scene hex glow?))
        materials {:C (mat "#4b4f58" false)
                   :O (mat "#e2555a" false)
                   :H (mat "#eef1f4" false)
                   :bond (mat "#9aa5b1" false)
                   :hbond (mat "#59e0c9" true)}

        R {:C 0.32 :O 0.34 :H 0.2}
        BOND-RADIUS 0.045
        HBOND-RADIUS 0.035

        sphere (fn [nm d material pos] (deck/make-sphere scene nm d material pos))
        bond (fn [nm a b] (deck/make-tube scene nm a b BOND-RADIUS (:bond materials)))

        ;; ethanol, static
        eth-group (js/BABYLON.TransformNode. "ethanol" scene)
        _ (doseq [k geo/eth-keys]
            (let [e (element-of k)
                  s (sphere (str "eth-" (name k)) (get R e) (get materials e)
                            (to-vec3 (get geo/eth k)))]
              (set! (.-parent s) eth-group)))
        _ (doseq [[a b] geo/eth-bonds]
            (let [t (bond (str "eth-bond-" (name a) "-" (name b))
                          (to-vec3 (get geo/eth a)) (to-vec3 (get geo/eth b)))]
              (set! (.-parent t) eth-group)))

        ;; waters: one TransformNode per water, translated each frame; local
        ;; shapes (fixed relative to that node) built once. Index-aligned with
        ;; geo/waters.
        anchors (mapv (fn [w]
                        (let [node (js/BABYLON.TransformNode. (str "water-" (:name w)) scene)
                              local (:local w)
                              o-pos (to-vec3 (:O local))
                              h1-pos (to-vec3 (:H1 local))
                              h2-pos (to-vec3 (:H2 local))]
                          (set! (.-position node) (to-vec3 (:base-o w)))
                          (doseq [m [(sphere (str (:name w) "-O") (:O R) (:O materials) o-pos)
                                     (sphere (str (:name w) "-H1") (:H R) (:H materials) h1-pos)
                                     (sphere (str (:name w) "-H2") (:H R) (:H materials) h2-pos)
                                     (bond (str (:name w) "-b1") o-pos h1-pos)
                                     (bond (str (:name w) "-b2") o-pos h2-pos)]]
                            (set! (.-parent m) node))
                          node))
                      geo/waters)

        ;; H-bond candidate tubes, index-aligned with geo/pairs.
        tubes (mapv (fn [_] (deck/make-hbond-tube scene HBOND-RADIUS (:hbond materials)))
                    geo/pairs)

        amp-input (el "amp")
        speed-input (el "speed")
        amp-value-el (el "amp-value")
        speed-value-el (el "speed-value")
        play-toggle (el "play")
        readout-count (el "count")
        readout-gives (el "gives")
        readout-takes (el "takes")
        readout-dist (el "dist")

        state (atom {:amplitude (js/parseFloat (.-value amp-input))
                     :speed (js/parseFloat (.-value speed-input))
                     :running (not deck/reduce-motion?)
                     :t-accum 0
                     :last-frame nil})]

    (set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))

    (.addEventListener amp-input "input"
      (fn []
        (let [x (js/parseFloat (.-value amp-input))]
          (swap! state assoc :amplitude x)
          (set! (.-textContent amp-value-el) (str (.toFixed x 2) " Å")))))

    (.addEventListener speed-input "input"
      (fn []
        (let [x (js/parseFloat (.-value speed-input))]
          (swap! state assoc :speed x)
          (set! (.-textContent speed-value-el) (str (.toFixed x 1) "×")))))

    (.addEventListener play-toggle "click"
      (fn []
        (swap! state update :running not)
        (set! (.-textContent play-toggle) (if (:running @state) "Pause" "Play"))))

    (letfn [(frame []
              (let [{:keys [running amplitude speed last-frame t-accum]} @state
                    now (js/performance.now)
                    lf (or last-frame now)
                    dt (js/Math.min (/ (- now lf) 1000) 0.05)
                    t' (if running (+ t-accum (* dt speed)) t-accum)
                    current (mapv #(geo/compute-current % t' amplitude) geo/waters)
                    active-count (atom 0)
                    ;; split the count by which side of the -OH is doing the
                    ;; work: ethanol's own hydroxyl H donating out, versus its
                    ;; oxygen accepting a water's hydrogen in
                    gives (atom 0)
                    takes (atom 0)
                    nearest (atom js/Infinity)]
                (swap! state assoc :last-frame now :t-accum t')

                (dotimes [i (count geo/waters)]
                  (.copyFrom (.-position (nth anchors i)) (to-vec3 (:O (nth current i)))))

                (dotimes [idx (count geo/pairs)]
                  (let [p (nth geo/pairs idx)
                        has-donor (some? (:donor p))
                        c (nth current (if has-donor (:donor p) (:acceptor p)))
                        H (if has-donor
                            (if (= (:donor-atom p) :H1) (:H1 c) (:H2 c))
                            (:H9 geo/eth))
                        D (if has-donor (:O c) (:O geo/eth))
                        A (if has-donor (:O geo/eth) (:O c))
                        dist-vec (v/sub A H)
                        dist (v/len dist-vec)
                        cos-angle (v/dot (v/norm (v/sub D H)) (v/norm dist-vec))
                        active (and (< dist geo/DIST-LIMIT) (< cos-angle geo/COS-LIMIT))
                        {:keys [mesh opts]} (nth tubes idx)]
                    (set! (.-isVisible mesh) active)
                    (when active
                      (.copyFrom (aget (.-path opts) 0) (to-vec3 H))
                      (.copyFrom (aget (.-path opts) 1) (to-vec3 A))
                      (set! (.-instance opts) mesh)
                      (js/BABYLON.MeshBuilder.CreateTube "hbond" opts)
                      (swap! active-count inc)
                      (if has-donor (swap! takes inc) (swap! gives inc)))
                    (when (< dist @nearest) (reset! nearest dist))))

                (set! (.-textContent readout-count) @active-count)
                (set! (.-textContent readout-gives) @gives)
                (set! (.-textContent readout-takes) @takes)
                (set! (.-className readout-count) (str "num" (if (zero? @active-count) " zero" "")))
                (set! (.-textContent readout-dist)
                      (if (js/isFinite @nearest) (str (.toFixed @nearest 2) " Å") "—"))))]

      (deck/slide-handle engine scene camera canvas frame))))
