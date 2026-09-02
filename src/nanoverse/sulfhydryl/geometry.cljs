(ns nanoverse.sulfhydryl.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]))

;; Pure chemistry for the sulfhydryl slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; Again two deposited structures. Beta-mercaptoethanol is PDB Chemical
;; Component Dictionary entry BME; the thing two of them turn into when they
;; are oxidised -- 2-hydroxyethyl disulfide -- is entry HED. Both verbatim in
;; structures/. Angstroms.
;;
;; Mercaptoethanol is an unusually good molecule to look at a thiol with,
;; because it carries the CONTROL on the same two carbons: an -OH on C1 and an
;; -SH on C2. Oxygen and sulfur are in the same column of the periodic table
;; and the two groups look identical on paper. The deposited geometry already
;; says they are not:
;;
;;     C1-O1  1.430 A        C2-S2  1.813 A
;;
;; Sulfur is a much bigger atom. It is also much less electronegative (2.58
;; against oxygen's 3.44), so an S-H bond is barely polarised and its hydrogen
;; is a poor thing to offer a hydrogen bond with.
;;
;; And then sulfur does the thing oxygen cannot: two of them join up into a
;; covalent S-S bridge, reversibly, under ordinary cellular conditions. That
;; is what holds an antibody together, and mercaptoethanol is the reagent
;; people add to protein samples precisely to break those bridges by
;; sacrificing its own.
;; ---------------------------------------------------------------------

(def bme-raw
  {:C1  {:e :C :x   0.4740 :y   0.0000 :z   1.0590}
   :C2  {:e :C :x  -0.6210 :y   0.0000 :z  -0.0070}
   :O1  {:e :O :x  -0.1250 :y   0.0000 :z   2.3570}   ; the hydroxyl -- the control
   :S2  {:e :S :x   0.1380 :y   0.0000 :z  -1.6540}   ; the sulfhydryl
   :H11 {:e :H :x   1.0920 :y   0.8900 :z   0.9450}
   :H12 {:e :H :x   1.0920 :y  -0.8900 :z   0.9450}
   :H21 {:e :H :x  -1.2400 :y  -0.8900 :z   0.1060}
   :H22 {:e :H :x  -1.2400 :y   0.8900 :z   0.1060}
   :HO1 {:e :H :x   0.5980 :y   0.0000 :z   2.9990}   ; strongly polar
   :HS2 {:e :H :x  -0.9650 :y   0.0000 :z  -2.4220}}) ; barely polar at all

(def bme-bonds
  [{:a :C1 :b :C2  :order 1}
   {:a :C1 :b :O1  :order 1}   ; 1.430 A
   {:a :C1 :b :H11 :order 1}
   {:a :C1 :b :H12 :order 1}
   {:a :C2 :b :S2  :order 1}   ; 1.813 A -- the same kind of bond, far longer
   {:a :C2 :b :H21 :order 1}
   {:a :C2 :b :H22 :order 1}
   {:a :O1 :b :HO1 :order 1}
   {:a :S2 :b :HS2 :order 1}])

(def hed-raw
  {:C1  {:e :C :x   0.2060 :y  -0.0470 :z   2.6800}
   :O1  {:e :O :x   0.3110 :y  -1.0420 :z   3.7000}
   :C2  {:e :C :x  -0.8300 :y  -0.4860 :z   1.6440}
   :S3  {:e :S :x  -0.9630 :y   0.7770 :z   0.3490}
   :S4  {:e :S :x   0.9640 :y   0.7750 :z  -0.3490}
   :C5  {:e :C :x   0.8290 :y  -0.4870 :z  -1.6440}
   :C6  {:e :C :x  -0.2060 :y  -0.0460 :z  -2.6800}
   :O6  {:e :O :x  -0.3120 :y  -1.0420 :z  -3.7000}
   :H11 {:e :H :x  -0.1020 :y   0.8980 :z   3.1250}
   :H12 {:e :H :x   1.1740 :y   0.0790 :z   2.1940}
   :HO1 {:e :H :x   0.9730 :y  -0.7270 :z   4.3300}
   :H21 {:e :H :x  -0.5200 :y  -1.4310 :z   1.1980}
   :H22 {:e :H :x  -1.7970 :y  -0.6120 :z   2.1290}
   :H51 {:e :H :x   1.7970 :y  -0.6140 :z  -2.1290}
   :H52 {:e :H :x   0.5190 :y  -1.4320 :z  -1.1980}
   :H61 {:e :H :x  -1.1740 :y   0.0800 :z  -2.1940}
   :H62 {:e :H :x   0.1030 :y   0.8980 :z  -3.1250}
   :HO6 {:e :H :x  -0.9740 :y  -0.7260 :z  -4.3300}})

(def hed-bonds
  [{:a :C1 :b :O1  :order 1}
   {:a :C1 :b :C2  :order 1}
   {:a :C1 :b :H11 :order 1}
   {:a :C1 :b :H12 :order 1}
   {:a :O1 :b :HO1 :order 1}
   {:a :C2 :b :S3  :order 1}
   {:a :C2 :b :H21 :order 1}
   {:a :C2 :b :H22 :order 1}
   {:a :S3 :b :S4  :order 1}   ; <- the disulfide bridge, 2.050 A
   {:a :S4 :b :C5  :order 1}
   {:a :C5 :b :C6  :order 1}
   {:a :C5 :b :H51 :order 1}
   {:a :C5 :b :H52 :order 1}
   {:a :C6 :b :O6  :order 1}
   {:a :C6 :b :H61 :order 1}
   {:a :C6 :b :H62 :order 1}
   {:a :O6 :b :HO6 :order 1}])

(def SS-LENGTH (v/len (v/sub (get hed-raw :S4) (get hed-raw :S3))))
(def CO-LENGTH (v/len (v/sub (get bme-raw :O1) (get bme-raw :C1))))
(def CS-LENGTH (v/len (v/sub (get bme-raw :S2) (get bme-raw :C2))))

;; ---------------------------------------------------------------------
;; The reduced pair.
;;
;; Each half is a whole, real mercaptoethanol -- with its own real S-H, which
;; the disulfide of course does not have. They are POSED by superposing each
;; onto one half of the real HED, then sliding both apart along the S-S axis
;; until the bridge is visibly broken.
;;
;; Say plainly what that is and is not: the internal geometry of each molecule
;; is measured, and the direction they separate along is measured, but two
;; free thiols in solution have no fixed relationship to each other at all.
;; They are drawn in the pose their disulfide would have so you can see
;; exactly which bond formed, not because they would sit like that.
;; ---------------------------------------------------------------------

(def SEPARATION 1.35) ; angstroms each, along the S-S axis

(def ss-axis (v/norm (v/sub (get hed-raw :S4) (get hed-raw :S3))))

(defn- rename-keys-with
  "Re-key a whole molecule so two copies can share one atoms map."
  [m tag]
  (into {} (for [k (keys m)] [(keyword (str tag "-" (name k))) (get m k)])))

(defn- retag-bonds [bs tag]
  (mapv (fn [b] {:a (keyword (str tag "-" (name (:a b))))
                 :b (keyword (str tag "-" (name (:b b))))
                 :order (:order b)})
        bs))

(defn- shift [m d]
  (into {} (for [k (keys m)] [k (merge (get m k) (v/add (get m k) d))])))

(def half-a
  (shift (sol/align bme-raw hed-raw [[:C1 :C1] [:C2 :C2] [:O1 :O1]])
         (v/scale ss-axis (- SEPARATION))))

(def half-b
  (shift (sol/align bme-raw hed-raw [[:C1 :C6] [:C2 :C5] [:O1 :O6]])
         (v/scale ss-axis SEPARATION)))

(def reduced-raw (merge (rename-keys-with half-a "A") (rename-keys-with half-b "B")))
(def reduced-bonds (vec (concat (retag-bonds bme-bonds "A") (retag-bonds bme-bonds "B"))))

;; one origin for both redox states, so toggling the bond does not shift the
;; molecule under the camera
(def origin (sol/centroid-of hed-raw [:S3 :S4]))

(def reduced
  (sol/hydrate
    {:label "2 × mercaptoethanol (reduced)"
     :atoms (sol/recenter reduced-raw origin)
     :bonds reduced-bonds
     :charge 0
     :acceptors [{:atom :A-O1 :site :hydroxyl} {:atom :B-O1 :site :hydroxyl}
                 {:atom :A-S2 :site :sulfhydryl :max-lp 1}
                 {:atom :B-S2 :site :sulfhydryl :max-lp 1}]
     :donors [{:h :A-HO1 :heavy :A-O1 :site :hydroxyl}
              {:h :B-HO1 :heavy :B-O1 :site :hydroxyl}
              {:h :A-HS2 :heavy :A-S2 :site :sulfhydryl}
              {:h :B-HS2 :heavy :B-S2 :site :sulfhydryl}]}
    {:seed 20260908}))

(def oxidized
  (sol/hydrate
    {:label "2-hydroxyethyl disulfide (oxidised)"
     :atoms (sol/recenter hed-raw origin)
     :bonds hed-bonds
     :charge 0
     ;; the two sulfurs are still there and still have lone pairs, but there
     ;; is no S-H left anywhere on the molecule
     :acceptors [{:atom :O1 :site :hydroxyl} {:atom :O6 :site :hydroxyl}
                 {:atom :S3 :site :disulfide :max-lp 1}
                 {:atom :S4 :site :disulfide :max-lp 1}]
     :donors [{:h :HO1 :heavy :O1 :site :hydroxyl}
              {:h :HO6 :heavy :O6 :site :hydroxyl}]}
    {:seed 20260909}))

(def states [reduced oxidized])
