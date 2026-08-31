(ns hydration-console.core)

;; ClojureScript (Squint) port of ../hydration-console.html's inline JS.
;; Same geometry, same bonding math, same tuned constants -- see that file's
;; comments for the chemistry rationale. This file just carries it over
;; idiomatically: vectors as plain maps, mutable per-frame state in one atom
;; instead of scattered module-level vars, Zdog itself accessed via plain
;; js/ interop since it's an inherently imperative, mutating API.

;; ---------------------------------------------------------------------
;; Real geometry, in angstroms. Ethanol: RCSB Chemical Component
;; Dictionary ideal coordinates (EOH). Water: standard gas-phase
;; geometry (O-H 0.9572 A, H-O-H 104.5 deg).
;; ---------------------------------------------------------------------
(def PX-PER-A 42)
(def OH-LEN 0.9572)
(def HOH-ANGLE (/ (* 104.5 js/Math.PI) 180))

(def eth-raw
  {:C1 {:x 0.0070 :y -0.5690 :z 0.0000}
   :C2 {:x -1.2850 :y 0.2500 :z 0.0000}
   :O {:x 1.1300 :y 0.3150 :z 0.0000}
   :H4 {:x 0.0390 :y -1.1970 :z 0.8900}    ; on C1
   :H5 {:x 0.0390 :y -1.1970 :z -0.8900}   ; on C1
   :H6 {:x -1.3170 :y 0.8780 :z 0.8900}    ; on C2
   :H7 {:x -1.3170 :y 0.8780 :z -0.8900}   ; on C2
   :H8 {:x -2.1420 :y -0.4240 :z 0.0000}   ; on C2
   :H9 {:x 1.9860 :y -0.1370 :z 0.0000}})  ; hydroxyl H, on O

;; vector helpers ---------------------------------------------------------
(defn add [a b] {:x (+ (:x a) (:x b)) :y (+ (:y a) (:y b)) :z (+ (:z a) (:z b))})
(defn sub [a b] {:x (- (:x a) (:x b)) :y (- (:y a) (:y b)) :z (- (:z a) (:z b))})
(defn scale [a s] {:x (* (:x a) s) :y (* (:y a) s) :z (* (:z a) s)})
(defn vdot [a b] (+ (* (:x a) (:x b)) (* (:y a) (:y b)) (* (:z a) (:z b))))
(defn vcross [a b]
  {:x (- (* (:y a) (:z b)) (* (:z a) (:y b)))
   :y (- (* (:z a) (:x b)) (* (:x a) (:z b)))
   :z (- (* (:x a) (:y b)) (* (:y a) (:x b)))})
(defn vlen [a] (js/Math.sqrt (vdot a a)))
(defn vnorm [a] (let [l (or (vlen a) 1)] {:x (/ (:x a) l) :y (/ (:y a) l) :z (/ (:z a) l)}))

;; Zdog only ever needs the *final* pixel-space point, and it needs a real
;; JS object (a Squint map isn't a plain {x,y,z} object with dot-accessible
;; properties) -- so this is the one place vectors leave Clojure-land.
(defn to-px [a]
  #js {:x (* (:x a) PX-PER-A) :y (* (:y a) PX-PER-A) :z (* (:z a) PX-PER-A)})

;; recenter ethanol on its own centroid so the illustration sits at (0,0,0)
(def eth-keys (keys eth-raw))
(def eth
  (let [centroid (scale (reduce add {:x 0 :y 0 :z 0} (vals eth-raw)) (/ 1 (count eth-raw)))]
    (into {} (for [k eth-keys] [k (sub (get eth-raw k) centroid)]))))

(def eth-bonds
  [[:C1 :C2] [:C1 :O] [:C1 :H4] [:C1 :H5]
   [:C2 :H6] [:C2 :H7] [:C2 :H8] [:O :H9]])

;; ---------------------------------------------------------------------
;; Water placement: each water's local +e1 axis is aimed back at
;; ethanol's oxygen, so its first hydrogen is built already pointing the
;; "right" way for a linear donor...acceptor geometry. water_acceptor
;; instead reuses ethanol's own O-H9 direction, since there ethanol is
;; the donor.
;; ---------------------------------------------------------------------
(defn build-basis [away-dir]
  (let [e1 (scale away-dir -1) ; points back toward ethanol O
        ref (if (> (js/Math.abs (:y e1)) 0.9) {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0})
        e2 (vnorm (vcross e1 ref))]
    {:e1 e1 :e2 e2}))

(defn water-atoms [basis]
  (let [h1 (scale (:e1 basis) OH-LEN)
        h2 (add (scale (:e1 basis) (* (js/Math.cos HOH-ANGLE) OH-LEN))
                (scale (:e2 basis) (* (js/Math.sin HOH-ANGLE) OH-LEN)))]
    {:O {:x 0 :y 0 :z 0} :H1 h1 :H2 h2}))

(def acceptor-dir (vnorm (sub (:H9 eth) (:O eth))))

;; O...O set so the *resting* donor-H...O distance sits just inside the
;; 2.5 A cutoff (see dist-limit below) -- close enough that the default
;; jitter amplitude carries it back out again, so bonds actually flicker
;; instead of sitting on permanently. Angle is ~180 deg by construction
;; regardless of this distance (see build-basis/water-atoms above).
(def water-defs
  [{:name "donor_1" :away (vnorm {:x -0.7 :y -0.9 :z 0.5}) :oo 3.26}
   {:name "donor_2" :away (vnorm {:x -0.9 :y 0.6 :z -0.4}) :oo 3.34}
   {:name "acceptor" :away acceptor-dir :oo 3.06}
   {:name "tail_1" :away (vnorm {:x 0.3 :y 0.9 :z 0.8}) :oo 3.6}
   {:name "tail_2" :away (vnorm {:x 0.9 :y 0.7 :z -0.6}) :oo 4.2}
   {:name "tail_3" :away (vnorm {:x -0.2 :y -0.3 :z -1.0}) :oo 5.0}])

;; seeded PRNG so the wobble is fixed across reloads but decorrelated
;; per water / axis / term
(defn mulberry32 [seed0]
  (let [seed (atom (bit-or seed0 0))]
    (fn []
      (swap! seed (fn [s] (bit-or (+ s 0x6D2B79F5) 0)))
      (let [s @seed
            t1 (js/Math.imul (bit-xor s (unsigned-bit-shift-right s 15)) (bit-or 1 s))
            t2 (bit-xor (+ t1 (js/Math.imul (bit-xor t1 (unsigned-bit-shift-right t1 7)) (bit-or 61 t1))) t1)]
        (/ (unsigned-bit-shift-right (bit-xor t2 (unsigned-bit-shift-right t2 14)) 0) 4294967296)))))

(def rng (mulberry32 20260830))

(def waters
  (vec (map
         (fn [wd]
           (let [basis (build-basis (:away wd))
                 local (water-atoms basis)
                 base-o (add (:O eth) (scale (:away wd) (:oo wd)))
                 wobble (vec (repeatedly 3
                               (fn []
                                 [{:freq (+ 0.6 (* (rng) 0.5)) :phase (* (rng) js/Math.PI 2) :amp 0.62}
                                  {:freq (+ 1.3 (* (rng) 0.9)) :phase (* (rng) js/Math.PI 2) :amp 0.38}])))]
             {:name (:name wd) :basis basis :local local :base-o base-o :wobble wobble}))
         water-defs)))

(defn jitter-for [water t]
  (into {}
        (map-indexed
          (fn [i ax]
            [ax (reduce +
                        (map (fn [term] (* (:amp term) (js/Math.sin (+ (* t (:freq term)) (:phase term)))))
                             (nth (:wobble water) i)))])
          [:x :y :z])))

;; candidate hydrogen-bond pairs, mirroring the Blender overlay: every
;; water gets checked as donor (both H's) and as acceptor.
(def pairs
  (vec (mapcat
         (fn [i _w]
           [{:donor i :donor-atom :H1 :acceptor nil}
            {:donor i :donor-atom :H2 :acceptor nil}
            {:donor nil :donor-atom :H9 :acceptor i}])
         (range)
         waters)))

;; ---------------------------------------------------------------------
;; Zdog scene
;; ---------------------------------------------------------------------
(def canvas (js/document.querySelector ".zdog-canvas"))
(def illo
  (js/Zdog.Illustration. #js {:element canvas
                               :zoom 1.6
                               :dragRotate true
                               :rotate #js {:x -0.28 :y 0.55}}))

(def R {:C 15 :O 16 :H 9})
(def COLOR {:C "#4b4f58" :O "#e2555a" :H "#eef1f4" :bond "#9aa5b1" :hbond "#59e0c9"})

(defn element-of [k] (keyword (subs (name k) 0 1))) ; :C1 -> :C, :H4 -> :H, :O -> :O

;; ethanol, static
(def eth-group (js/Zdog.Anchor. #js {:addTo illo}))
(doseq [k eth-keys]
  (let [el (element-of k)]
    (js/Zdog.Shape. #js {:addTo eth-group
                          :translate (to-px (get eth k))
                          :stroke (get R el)
                          :color (get COLOR el)})))
(doseq [[a b] eth-bonds]
  (js/Zdog.Shape. #js {:addTo eth-group
                        :path #js [(to-px (get eth a)) (to-px (get eth b))]
                        :stroke 6
                        :color (:bond COLOR)}))

;; waters: one Anchor per water, translated each frame; local shapes
;; (fixed relative to that Anchor) built once. Index-aligned with `waters`.
(def anchors
  (mapv (fn [w]
          (let [a (js/Zdog.Anchor. #js {:addTo illo :translate (to-px (:base-o w))})
                local (:local w)]
            (js/Zdog.Shape. #js {:addTo a :translate (to-px (:O local)) :stroke (:O R) :color (:O COLOR)})
            (js/Zdog.Shape. #js {:addTo a :translate (to-px (:H1 local)) :stroke (:H R) :color (:H COLOR)})
            (js/Zdog.Shape. #js {:addTo a :translate (to-px (:H2 local)) :stroke (:H R) :color (:H COLOR)})
            (js/Zdog.Shape. #js {:addTo a :path #js [(to-px (:O local)) (to-px (:H1 local))] :stroke 5 :color (:bond COLOR)})
            (js/Zdog.Shape. #js {:addTo a :path #js [(to-px (:O local)) (to-px (:H2 local))] :stroke 5 :color (:bond COLOR)})
            a))
        waters))

;; index-aligned with `pairs`
(def tubes
  (mapv (fn [_] (js/Zdog.Shape. #js {:addTo illo
                                      :path #js [#js {:x 0 :y 0 :z 0} #js {:x 0 :y 0 :z 0}]
                                      :stroke 5
                                      :color (:hbond COLOR)
                                      :visible false}))
        pairs))

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

;; one state atom, replacing the JS version's scattered module-level vars
;; (amplitude, speed, running, tAccum, lastFrame)
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

;; ---------------------------------------------------------------------
;; animation loop
;; ---------------------------------------------------------------------
(def DIST-LIMIT 2.5) ; angstrom, donor-H...acceptor
(def COS-LIMIT -0.5) ; cos(120 deg): require a roughly linear D-H...A

(defn compute-current [w t amp]
  (let [j (jitter-for w t)
        o (add (:base-o w) (scale j amp))
        local (:local w)]
    {:O o :H1 (add o (:H1 local)) :H2 (add o (:H2 local))}))

(defn frame [now]
  (let [{:keys [running amplitude speed last-frame t-accum]} @state
        lf (or last-frame now)
        dt (js/Math.min (/ (- now lf) 1000) 0.05)
        t-accum' (if running (+ t-accum (* dt speed)) t-accum)
        current (mapv #(compute-current % t-accum' amplitude) waters)
        active-count (atom 0)
        nearest (atom js/Infinity)]
    (swap! state assoc :last-frame now :t-accum t-accum')

    (dotimes [i (count waters)]
      (.set (.-translate (nth anchors i)) (to-px (:O (nth current i)))))

    (dotimes [idx (count pairs)]
      (let [p (nth pairs idx)
            has-donor (some? (:donor p))
            w-idx (if has-donor (:donor p) (:acceptor p))
            c (nth current w-idx)
            H (if has-donor
                (if (= (:donor-atom p) :H1) (:H1 c) (:H2 c))
                (:H9 eth))
            D (if has-donor (:O c) (:O eth))
            A (if has-donor (:O eth) (:O c))
            dist-vec (sub A H)
            dist (vlen dist-vec)
            vec-hd (vnorm (sub D H))
            vec-ha (vnorm dist-vec)
            cos-angle (vdot vec-hd vec-ha)
            active (and (< dist DIST-LIMIT) (< cos-angle COS-LIMIT))
            tube (nth tubes idx)]
        (set! (.-visible tube) active)
        (when active
          (aset (.-path tube) 0 (to-px H))
          (aset (.-path tube) 1 (to-px A))
          (.updatePath tube)
          (swap! active-count inc))
        (when (< dist @nearest) (reset! nearest dist))))

    (set! (.-textContent readout-count) @active-count)
    (set! (.-className readout-count) (str "num" (if (zero? @active-count) " zero" "")))
    (set! (.-textContent readout-dist)
          (if (js/isFinite @nearest) (str (.toFixed @nearest 2) " Å") "—"))

    (.updateRenderGraph illo)
    (js/requestAnimationFrame frame)))

(js/requestAnimationFrame frame)
