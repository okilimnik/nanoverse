(ns nanoverse.ketone.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]))

;; Pure chemistry for the ketone slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; Acetone, PDB Chemical Component Dictionary entry ACN, ideal coordinates,
;; kept verbatim in structures/ACN.cif. Angstroms.
;;
;; Unlike the acetaldehyde on the previous slide, this one IS a deposited
;; component -- acetone appears in real crystal structures as a cryoprotectant
;; and solvent, so the CCD carries it.
;;
;; A ketone and an aldehyde are the same C=O. The only difference is what sits
;; on either side of it: an aldehyde has a carbon and a *hydrogen*, a ketone
;; has a carbon and a *carbon*. Everything on this slide follows from that one
;; swap.
;; ---------------------------------------------------------------------

(def raw
  {:C   {:e :C :x   0.0000 :y  -0.0760 :z   0.0020}   ; carbonyl carbon
   :O   {:e :O :x  -0.0000 :y  -1.2840 :z  -0.0000}   ; carbonyl oxygen -- ACCEPTOR ONLY
   :C1  {:e :C :x   1.3050 :y   0.6770 :z  -0.0000}   ; methyl
   :C2  {:e :C :x  -1.3050 :y   0.6770 :z  -0.0000}   ; the second methyl -- where an
   :H11 {:e :H :x   1.6170 :y   0.8600 :z  -1.0280}   ;   aldehyde would carry an H
   :H12 {:e :H :x   1.1760 :y   1.6290 :z   0.5150}
   :H13 {:e :H :x   2.0660 :y   0.0870 :z   0.5110}
   :H21 {:e :H :x  -1.6220 :y   0.8570 :z   1.0270}
   :H22 {:e :H :x  -1.1730 :y   1.6300 :z  -0.5120}
   :H23 {:e :H :x  -2.0630 :y   0.0890 :z  -0.5170}})

(def bonds
  [{:a :C  :b :O   :order 2}    ; <- the carbonyl, 1.208 A in this file
   {:a :C  :b :C1  :order 1}
   {:a :C  :b :C2  :order 1}
   {:a :C1 :b :H11 :order 1}
   {:a :C1 :b :H12 :order 1}
   {:a :C1 :b :H13 :order 1}
   {:a :C2 :b :H21 :order 1}
   {:a :C2 :b :H22 :order 1}
   {:a :C2 :b :H23 :order 1}])

(def atoms (sol/recenter raw (sol/centroid-of raw [:C :O :C1 :C2])))

;; C=O bond length, read back out of the coordinates rather than quoted, so
;; the number on screen and the geometry on screen cannot drift apart.
(def co-length (v/len (v/sub (get atoms :O) (get atoms :C))))

;; ---------------------------------------------------------------------
;; The molecule's own account of what it can do with water.
;;
;; `donors` is empty. Acetone has six hydrogens and not one of them is polar:
;; every single one is on a carbon. So no candidate pair can ever put this
;; molecule on the donating side and the GIVES readout is zero by
;; construction -- the same reason it was zero for the aldehyde, arrived at
;; from a molecule with no C-H on the carbonyl at all.
;;
;; The two methyls are declared as hydrophobic sites: waters get placed
;; against them at van der Waals contact precisely so you can watch nothing
;; happen there.
;; ---------------------------------------------------------------------

(def state
  (sol/hydrate
    {:label "acetone"
     :atoms atoms
     :bonds bonds
     :charge 0
     :acceptors [{:atom :O :site :carbonyl}]
     :donors []}
    {:seed 20260902
     :hydrophobic [{:atom :C1 :site :methyl :n 2}
                   {:atom :C2 :site :methyl :n 2}]}))

(def methyl-water-count
  (count (filter #(= (:site %) :methyl) (:waters state))))
