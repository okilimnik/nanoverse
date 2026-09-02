(ns nanoverse.hydroxyl.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]))

;; Pure chemistry/physics for the hydroxyl scene -- no rendering-engine
;; dependency at all. The water model, the wobble and the hydrogen-bond test
;; all live in nanoverse.solvent, shared with every other group slide; what is
;; left here is ethanol, and this slide's own deliberate choice about where to
;; put its waters.
;;
;; ---------------------------------------------------------------------
;; Real geometry, in angstroms. Ethanol: RCSB Chemical Component Dictionary
;; ideal coordinates (EOH), kept verbatim in structures/ethanol.pdb. Water is
;; standard gas-phase geometry (see solvent/OH-LEN, solvent/HOH-ANGLE).
;; ---------------------------------------------------------------------

(def eth-raw
  {:C1 {:e :C :x  0.0070 :y -0.5690 :z  0.0000}
   :C2 {:e :C :x -1.2850 :y  0.2500 :z  0.0000}
   :O  {:e :O :x  1.1300 :y  0.3150 :z  0.0000}
   :H4 {:e :H :x  0.0390 :y -1.1970 :z  0.8900}    ; on C1
   :H5 {:e :H :x  0.0390 :y -1.1970 :z -0.8900}    ; on C1
   :H6 {:e :H :x -1.3170 :y  0.8780 :z  0.8900}    ; on C2
   :H7 {:e :H :x -1.3170 :y  0.8780 :z -0.8900}    ; on C2
   :H8 {:e :H :x -2.1420 :y -0.4240 :z  0.0000}    ; on C2
   :H9 {:e :H :x  1.9860 :y -0.1370 :z  0.0000}})  ; hydroxyl H, on O

(def eth-keys (keys eth-raw))

;; recentred on the centroid of ALL atoms (not just the heavy ones), which is
;; what this slide has always done -- nothing here gains or loses a hydrogen,
;; so there is no pH-driven drift to protect against
(def eth (sol/recenter eth-raw (sol/centroid-of eth-raw eth-keys)))

(def eth-bonds
  [{:a :C1 :b :C2 :order 1}
   {:a :C1 :b :O  :order 1}
   {:a :C1 :b :H4 :order 1}
   {:a :C1 :b :H5 :order 1}
   {:a :C2 :b :H6 :order 1}
   {:a :C2 :b :H7 :order 1}
   {:a :C2 :b :H8 :order 1}
   {:a :O  :b :H9 :order 1}])

;; ---------------------------------------------------------------------
;; Water placement.
;;
;; This slide places its waters BY HAND, which every later slide in the deck
;; does not -- they hand their molecule to solvent/hydrate and let the
;; lone-pair geometry decide. The reason is that this scene is calibrated: the
;; resting O...O distances are set so the donor-H...O distance sits just
;; INSIDE the 2.5 A cutoff, close enough that the default jitter amplitude
;; carries it back out again. That is what makes the count breathe instead of
;; sitting on permanently, and the breathing is the whole lesson.
;;
;; It is tuned, not measured, and the slide says so in its own LIMITS. See
;; solvent/place.
;;
;; Each water's local +e1 axis is aimed back at ethanol's oxygen, so its first
;; hydrogen is already pointing the right way for a linear donor...acceptor
;; geometry -- role :donor for all six, including the one that mainly accepts
;; ethanol's own H9, since the basis only decides which way the water faces
;; and both directions are tested for every water anyway.
;; ---------------------------------------------------------------------

(def acceptor-dir (v/norm (v/sub (:H9 eth) (:O eth))))

(def water-defs
  [{:name "donor_1"  :site :hydroxyl :role :donor :anchor :O :oo 3.26
    :away (v/norm {:x -0.7 :y -0.9 :z  0.5})}
   {:name "donor_2"  :site :hydroxyl :role :donor :anchor :O :oo 3.34
    :away (v/norm {:x -0.9 :y  0.6 :z -0.4})}
   {:name "acceptor" :site :hydroxyl :role :donor :anchor :O :oo 3.06
    :away acceptor-dir}
   {:name "tail_1"   :site :bulk     :role :donor :anchor :O :oo 3.6
    :away (v/norm {:x  0.3 :y  0.9 :z  0.8})}
   {:name "tail_2"   :site :bulk     :role :donor :anchor :O :oo 4.2
    :away (v/norm {:x  0.9 :y  0.7 :z -0.6})}
   {:name "tail_3"   :site :bulk     :role :donor :anchor :O :oo 5.0
    :away (v/norm {:x -0.2 :y -0.3 :z -1.0})}])

;; ---------------------------------------------------------------------
;; What the group can do, declared the same way every other slide declares it.
;;
;; Unlike the aldehyde, ketone and everything downstream of them, this list
;; has an entry on BOTH sides: the oxygen accepts on its lone pairs and the
;; hydroxyl hydrogen donates. That symmetry is the slide.
;; ---------------------------------------------------------------------

(def state
  (sol/place
    {:label "ethanol"
     :atoms eth
     :bonds eth-bonds
     :charge 0
     :acceptors [{:atom :O :site :hydroxyl}]
     :donors [{:h :H9 :heavy :O :site :hydroxyl}]}
    water-defs
    {:seed 20260830}))
