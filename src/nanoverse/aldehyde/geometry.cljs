(ns nanoverse.aldehyde.geometry
  (:require [nanoverse.vec3 :as v]))

;; Pure chemistry/physics for the aldehyde scene -- no rendering-engine
;; dependency. The companion babylon_core.cljs does all the drawing.
;;
;; ---------------------------------------------------------------------
;; Real geometry, in angstroms. Acetaldehyde (CH3-CHO), PubChem CID 177
;; 3D conformer, kept verbatim in structures/acetaldehyde.sdf. Water at
;; standard gas-phase geometry (O-H 0.9572 A, H-O-H 104.5 deg).
;;
;; Provenance note: acetaldehyde is too small and volatile to appear as a
;; PDB ligand, so unlike ethanol (EOH) this is a *computed* conformer
;; rather than experimental coordinates. It reproduces the microwave
;; structure closely -- C=O 1.226 vs 1.216 A, C-C 1.498 vs 1.501 A,
;; C-C=O 123.4 vs 124.1 deg -- and its CHO group is planar to 0.0007 A,
;; which is what the sp2 lone-pair construction below relies on.
;;
;; Acetaldehyde is what the ethanol on the previous slide becomes: alcohol
;; dehydrogenase oxidises the -OH to a -CHO. Same two carbons, one group
;; swapped, and the hydrogen-bonding behaviour changes completely.
;; ---------------------------------------------------------------------

(def OH-LEN 0.9572)
(def HOH-ANGLE (/ (* 104.5 js/Math.PI) 180))

(def mol-raw
  {:C1  {:x  0.1130 :y -0.4226 :z  0.0000}   ; carbonyl (aldehyde) carbon
   :O1  {:x  1.1443 :y  0.2412 :z  0.0000}   ; carbonyl oxygen -- ACCEPTOR ONLY
   :H1  {:x  0.1478 :y -1.5252 :z -0.0007}   ; aldehyde C-H (not an H-bond donor)
   :C2  {:x -1.2574 :y  0.1815 :z  0.0000}   ; methyl carbon
   :H2a {:x -1.7938 :y -0.1493 :z  0.8924}
   :H2b {:x -1.1865 :y  1.2719 :z  0.0016}
   :H2c {:x -1.7928 :y -0.1468 :z -0.8938}})

(def mol-keys (keys mol-raw))

;; recenter on the molecule's own centroid so it sits at the origin
(def mol
  (let [c (v/centroid (vals mol-raw))]
    (into {} (for [k mol-keys] [k (v/sub (get mol-raw k) c)]))))

;; Bond orders come straight from the SDF's bond block. The C1=O1 double
;; bond is the whole point of this scene, so it is carried as data rather
;; than special-cased in the renderer.
(def mol-bonds
  [{:a :C1 :b :O1  :order 2}   ; <- the carbonyl
   {:a :C1 :b :H1  :order 1}   ; <- the aldehyde hydrogen
   {:a :C1 :b :C2  :order 1}
   {:a :C2 :b :H2a :order 1}
   {:a :C2 :b :H2b :order 1}
   {:a :C2 :b :H2c :order 1}])

;; ---------------------------------------------------------------------
;; Carbonyl geometry. The carbonyl carbon is sp2, so C1 / O1 / H1 / C2 are
;; coplanar; the oxygen's two lone pairs lie in that same plane, about
;; 120 deg from the O->C direction. Waters that bond to C=O come in along
;; those directions, which is why they are computed rather than eyeballed.
;; ---------------------------------------------------------------------

(def carbonyl-axis (v/norm (v/sub (:O1 mol) (:C1 mol))))     ; C -> O
(def carbonyl-normal
  (v/norm (v/cross (v/sub (:C2 mol) (:C1 mol))
                   (v/sub (:O1 mol) (:C1 mol)))))

(defn- in-plane-at [deg]
  (v/rotate (v/quat-axis-angle carbonyl-normal (/ (* deg js/Math.PI) 180))
            carbonyl-axis))

(def lone-pair-a (in-plane-at  60))
(def lone-pair-b (in-plane-at -60))

;; direction out past the methyl group, away from the carbonyl
(def methyl-dir (v/norm (v/sub (:C2 mol) (:C1 mol))))

;; ---------------------------------------------------------------------
;; Waters.
;;
;; Two sit on the carbonyl's lone pairs and point an O-H at the oxygen.
;; The rest are placed around the methyl end and in bulk -- they are here
;; precisely because nothing ever happens to them. A -CH3 has no lone
;; pairs to accept with and no polar hydrogen to donate, so no matter how
;; close those waters drift, no bond appears.
;;
;; O...O distances are real hydrogen-bond distances (~2.8 A), not tuned to
;; sit at the edge of the cutoff. Bonds break here because the tumble
;; swings the hydrogen off-axis, not because the resting geometry was
;; rigged.
;; ---------------------------------------------------------------------

(def water-defs
  [{:name "lp-a"     :anchor :O1 :away lone-pair-a :oo 2.78 :role :donor :site :carbonyl}
   {:name "lp-b"     :anchor :O1 :away lone-pair-b :oo 2.84 :role :donor :site :carbonyl}
   {:name "methyl-1" :anchor :C2 :away (v/norm (v/add methyl-dir {:x 0.0 :y 0.5 :z 0.6}))
    :oo 3.7 :role :donor :site :methyl}
   {:name "methyl-2" :anchor :C2 :away (v/norm (v/add methyl-dir {:x 0.1 :y -0.6 :z -0.5}))
    :oo 3.8 :role :donor :site :methyl}
   {:name "bulk-1"   :anchor :O1 :away (v/norm {:x 0.5 :y -0.9 :z 0.7}) :oo 4.6
    :role :donor :site :bulk}
   {:name "bulk-2"   :anchor :C1 :away (v/norm {:x -0.2 :y 0.7 :z -1.0}) :oo 4.4
    :role :donor :site :bulk}])

(defn build-basis
  "Orthonormal pair for a water's own frame. :donor points e1 back at the
   molecule, so the water leads with one of its hydrogens."
  [away-dir role]
  (let [e1 (if (= role :donor) (v/scale away-dir -1) away-dir)
        ref (if (> (js/Math.abs (:y e1)) 0.9) {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0})
        e2 (v/norm (v/cross e1 ref))]
    {:e1 e1 :e2 e2}))

(defn water-atoms [basis]
  {:O {:x 0 :y 0 :z 0}
   :H1 (v/scale (:e1 basis) OH-LEN)
   :H2 (v/add (v/scale (:e1 basis) (* (js/Math.cos HOH-ANGLE) OH-LEN))
              (v/scale (:e2 basis) (* (js/Math.sin HOH-ANGLE) OH-LEN)))})

;; seeded PRNG -- fixed across reloads, decorrelated per water/axis/term
(defn mulberry32 [seed0]
  (let [seed (atom (bit-or seed0 0))]
    (fn []
      (swap! seed (fn [s] (bit-or (+ s 0x6D2B79F5) 0)))
      (let [s @seed
            t1 (js/Math.imul (bit-xor s (unsigned-bit-shift-right s 15)) (bit-or 1 s))
            t2 (bit-xor (+ t1 (js/Math.imul (bit-xor t1 (unsigned-bit-shift-right t1 7)) (bit-or 61 t1))) t1)]
        (/ (unsigned-bit-shift-right (bit-xor t2 (unsigned-bit-shift-right t2 14)) 0) 4294967296)))))

(def rng (mulberry32 20260831))

(def waters
  (vec (map
         (fn [wd]
           (let [basis (build-basis (:away wd) (:role wd))
                 local (water-atoms basis)
                 base-o (v/add (get mol (:anchor wd)) (v/scale (:away wd) (:oo wd)))
                 wobble (vec (repeatedly 3
                               (fn []
                                 [{:freq (+ 0.6 (* (rng) 0.5)) :phase (* (rng) js/Math.PI 2) :amp 0.62}
                                  {:freq (+ 1.3 (* (rng) 0.9)) :phase (* (rng) js/Math.PI 2) :amp 0.38}])))
                 tumble-axis (v/norm {:x (- (rng) 0.5) :y (- (rng) 0.5) :z (- (rng) 0.5)})]
             {:name (:name wd) :site (:site wd) :role (:role wd)
              :basis basis :local local :base-o base-o :wobble wobble
              :tumble-axis tumble-axis
              :tumble-freq (+ 0.5 (* (rng) 0.7))
              :tumble-phase (* (rng) js/Math.PI 2)}))
         water-defs)))

(defn jitter-for [water t]
  (into {}
        (map-indexed
          (fn [i ax]
            [ax (reduce +
                        (map (fn [term] (* (:amp term) (js/Math.sin (+ (* t (:freq term)) (:phase term)))))
                             (nth (:wobble water) i)))])
          [:x :y :z])))

(defn compute-current
  "World-space state of one water at time t. `amp` is translational jitter in
   angstroms, `tumble-deg` the peak rotation of the whole molecule about its
   own axis. Returns the quaternion too, so the renderer orients the drawn
   water with the exact same rotation this used for the hydrogens."
  [w t amp tumble-deg]
  (let [o (v/add (:base-o w) (v/scale (jitter-for w t) amp))
        theta (* (/ (* tumble-deg js/Math.PI) 180)
                 (js/Math.sin (+ (* t (:tumble-freq w)) (:tumble-phase w))))
        q (v/quat-axis-angle (:tumble-axis w) theta)
        local (:local w)]
    {:O o
     :H1 (v/add o (v/rotate q (:H1 local)))
     :H2 (v/add o (v/rotate q (:H2 local)))
     :quat q}))

;; ---------------------------------------------------------------------
;; Hydrogen-bond candidates.
;;
;; The asymmetry is the lesson, and it is expressed here as data rather
;; than as prose: `molecule-donors` is EMPTY. Acetaldehyde has no polar
;; hydrogen anywhere on it -- the only H on the carbonyl carbon is a C-H,
;; and carbon is not electronegative enough to make it a donor. So no
;; candidate pair can ever have this molecule on the donating side, and
;; the "gives" readout is pinned at zero by construction, not by a
;; threshold that happens never to be met.
;;
;; Contrast the hydroxyl slide, where the same list has one entry.
;; ---------------------------------------------------------------------

(def acceptor-sites
  [{:atom :O1 :site :carbonyl}])

(def molecule-donors [])

(def pairs
  (vec
    (concat
      ;; water donates one of its two hydrogens to our carbonyl oxygen
      (for [i (range (count waters))
            a acceptor-sites
            h [:H1 :H2]]
        {:kind :water-donates :water i :water-h h :acceptor (:atom a) :site (:site a)})
      ;; nothing here, on purpose -- see above
      (for [i (range (count waters))
            d molecule-donors]
        {:kind :molecule-donates :water i :donor-h (:h d) :donor-heavy (:heavy d) :site (:site d)}))))

(def DIST-LIMIT 2.5)  ; angstrom, donor-H...acceptor
(def COS-LIMIT -0.5)  ; cos(120 deg): require a roughly linear D-H...A

(defn evaluate
  "Geometry of one candidate pair against the current water positions.
   Returns the H...A distance, the D-H...A angle, and which of the two
   tests (if either) is the reason it is not a bond."
  [pair current]
  (let [c (nth current (:water pair))
        [D H A] (if (= (:kind pair) :water-donates)
                  [(:O c) (get c (:water-h pair)) (get mol (:acceptor pair))]
                  [(get mol (:donor-heavy pair)) (get mol (:donor-h pair)) (:O c)])
        d-vec (v/sub A H)
        dist (v/len d-vec)
        cos-a (v/dot (v/norm (v/sub D H)) (v/norm d-vec))
        near? (< dist DIST-LIMIT)
        linear? (< cos-a COS-LIMIT)]
    {:H H :A A :dist dist
     ;; the D-H...A angle itself: 180 deg is perfectly linear, and the test
     ;; below wants it above 120
     :angle (/ (* (js/Math.acos (js/Math.max -1 (js/Math.min 1 cos-a))) 180) js/Math.PI)
     :active (and near? linear?)
     ;; "close enough to touch, but pointing the wrong way" -- the case the
     ;; distance-only picture gets wrong
     :bent (and near? (not linear?))
     :kind (:kind pair)
     :site (:site pair)}))
