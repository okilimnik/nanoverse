(ns nanoverse.amino.geometry
  (:require [nanoverse.solvent :as sol]))

;; Pure chemistry for the amino slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; Methylamine, PDB Chemical Component Dictionary entry NME, ideal
;; coordinates, kept verbatim in structures/NME.cif. Angstroms.
;;
;; The carboxyl on the previous slide was an acid: it GIVES a proton away and
;; goes negative. An amine is the opposite, a base: it TAKES a proton and goes
;; positive. Same slider, same Henderson-Hasselbalch, opposite direction.
;;
;; And the interesting part is what the amine gives up in exchange. A neutral
;; -NH2 has a lone pair on the nitrogen, so it can accept a hydrogen bond as
;; well as donate two. Protonating it uses that lone pair up -- the proton is
;; bonded to it. An -NH3+ has three hydrogens to give and nothing left to take
;; with. You can watch that happen: TAKES drops to zero the moment the slider
;; goes under the pKa.
;; ---------------------------------------------------------------------

(def raw
  {:N   {:e :N :x   0.7200 :y   0.0000 :z  -0.0670}   ; the lone pair lives here
   :C   {:e :C :x  -0.7470 :y  -0.0000 :z   0.0110}
   :HN1 {:e :H :x   1.1050 :y   0.8320 :z   0.3560}   ; polar: donates
   :HN2 {:e :H :x   1.1050 :y  -0.8310 :z   0.3560}   ; polar: donates
   :H1  {:e :H :x  -1.1380 :y  -0.8720 :z  -0.5130}   ; on carbon: does nothing
   :H2  {:e :H :x  -1.0550 :y  -0.0360 :z   1.0560}
   :H3  {:e :H :x  -1.1350 :y   0.9070 :z  -0.4520}})

(def bonds
  [{:a :N :b :C   :order 1}   ; 1.469 A
   {:a :N :b :HN1 :order 1}
   {:a :N :b :HN2 :order 1}
   {:a :C :b :H1  :order 1}
   {:a :C :b :H2  :order 1}
   {:a :C :b :H3  :order 1}])

(def origin (sol/centroid-of raw [:N :C]))
(def atoms (sol/recenter raw origin))

;; ---------------------------------------------------------------------
;; pKa 10.66 -- for the methylammonium ion CH3NH3+ giving its proton back.
;; That is far ABOVE cell pH, which is the whole point: at pH 7 an amine sits
;; on the protonated, positively charged side of its own equilibrium, about
;; 99.98% of the time. Lysine side chains and every protein N-terminus are
;; positively charged for exactly this reason.
;; ---------------------------------------------------------------------

(def PKA 10.66)
(def pkas [PKA])

;; The protonated form. The third N-H is CONSTRUCTED, not measured: it is
;; placed at the one tetrahedral vertex the existing three bonds leave open,
;; at a standard 1.010 A N-H. See solvent/protonate -- the rest of the
;; molecule is not re-relaxed afterwards, though a real protonation does pull
;; the N-C bond in slightly.
(def acid-base-state
  (sol/protonate {:atoms atoms :bonds bonds} :N :HN3))

(def acid
  (sol/hydrate
    (merge acid-base-state
           {:label "methylammonium (CH3NH3+)"
            :charge 1
            ;; no acceptors: the lone pair is now holding a proton. This list
            ;; is empty as a statement, and solvent/lone-pair-dirs would agree
            ;; anyway -- a nitrogen with four neighbours has no pair left.
            :acceptors []
            :donors [{:h :HN1 :heavy :N :site :amino}
                     {:h :HN2 :heavy :N :site :amino}
                     {:h :HN3 :heavy :N :site :amino}]
            :charges {:N 1}})
    {:seed 20260905 :hydrophobic [{:atom :C :site :methyl :n 2}]}))

(def base
  (sol/hydrate
    {:label "methylamine (CH3NH2)"
     :atoms atoms :bonds bonds :charge 0
     :acceptors [{:atom :N :site :amino}]
     :donors [{:h :HN1 :heavy :N :site :amino}
              {:h :HN2 :heavy :N :site :amino}]}
    {:seed 20260906 :hydrophobic [{:atom :C :site :methyl :n 2}]}))

;; ordered by protons released, so index 0 is the acid -- the same convention
;; the carboxyl slide uses, which is what lets both share scene/titration!
(def states [acid base])
