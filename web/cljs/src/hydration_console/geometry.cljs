(ns hydration-console.geometry)

;; Pure chemistry/physics for the hydration-console scene -- no rendering
;; engine dependency at all. Extracted verbatim from core.cljs (the Zdog
;; version) so both engines render the exact same molecule. See that
;; file's original comments for the chemistry rationale; kept here too
;; where they explain a non-obvious constant.

;; ---------------------------------------------------------------------
;; Real geometry, in angstroms. Ethanol: RCSB Chemical Component
;; Dictionary ideal coordinates (EOH). Water: standard gas-phase
;; geometry (O-H 0.9572 A, H-O-H 104.5 deg).
;; ---------------------------------------------------------------------
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

;; candidate hydrogen-bond pairs: every water gets checked as donor (both
;; H's) and as acceptor.
(def pairs
  (vec (mapcat
         (fn [i _w]
           [{:donor i :donor-atom :H1 :acceptor nil}
            {:donor i :donor-atom :H2 :acceptor nil}
            {:donor nil :donor-atom :H9 :acceptor i}])
         (range)
         waters)))

(def DIST-LIMIT 2.5) ; angstrom, donor-H...acceptor
(def COS-LIMIT -0.5) ; cos(120 deg): require a roughly linear D-H...A

(defn compute-current [w t amp]
  (let [j (jitter-for w t)
        o (add (:base-o w) (scale j amp))
        local (:local w)]
    {:O o :H1 (add o (:H1 local)) :H2 (add o (:H2 local))}))
