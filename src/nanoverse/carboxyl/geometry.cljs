(ns nanoverse.carboxyl.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]))

;; Pure chemistry for the carboxyl slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; TWO deposited structures, not one. Acetic acid is PDB Chemical Component
;; Dictionary entry ACY; its conjugate base, the acetate ion, is a separate
;; entry, ACT. Both are kept verbatim in structures/. Angstroms.
;;
;; Having both matters, because the interesting thing about a carboxyl group
;; is that it does not stay put. Above its pKa it hands its proton to water
;; and becomes an anion -- and that is not just "the same molecule minus an
;; H". The two C-O bonds change. Read the numbers straight out of the two
;; files:
;;
;;     acetic acid   C=O 1.208 A    C-OH  1.342 A     (one double, one single)
;;     acetate       C-O 1.220 A    C-O   1.220 A     (identical)
;;
;; Losing the proton leaves the negative charge shared across both oxygens,
;; and the two bonds become the same bond. Nobody tuned that; it is what the
;; deposited coordinates say, and the slide reports it live off the geometry.
;; ---------------------------------------------------------------------

(def acid-raw
  {:C   {:e :C :x   0.0120 :y  -0.0560 :z   0.0020}   ; carboxyl carbon
   :O   {:e :O :x   0.2610 :y  -1.2380 :z  -0.0000}   ; the C=O oxygen
   :OXT {:e :O :x   1.0110 :y   0.8400 :z  -0.0000}   ; the C-O-H oxygen
   :CH3 {:e :C :x  -1.4210 :y   0.4120 :z  -0.0000}
   :HXT {:e :H :x   1.9130 :y   0.4930 :z  -0.0030}   ; <- the acidic proton
   :H1  {:e :H :x  -1.7680 :y   0.5230 :z   1.0270}
   :H2  {:e :H :x  -1.4890 :y   1.3720 :z  -0.5120}
   :H3  {:e :H :x  -2.0420 :y  -0.3200 :z  -0.5170}})

(def base-raw
  {:C   {:e :C :x  -0.0720 :y   0.0000 :z   0.0000}
   :O   {:e :O :x  -0.6820 :y   1.0560 :z   0.0000}
   :OXT {:e :O :x  -0.6820 :y  -1.0560 :z   0.0000}
   :CH3 {:e :C :x   1.4350 :y   0.0000 :z   0.0000}
   :H1  {:e :H :x   1.7990 :y   0.0000 :z   1.0280}
   :H2  {:e :H :x   1.7990 :y  -0.8900 :z  -0.5140}
   :H3  {:e :H :x   1.7990 :y   0.8900 :z  -0.5140}})

;; ACT arrives in its own coordinate frame, unrelated to ACY's. Put it on top
;; of the acid before anything else, or crossing the pKa would spin the
;; molecule around -- motion that looks like chemistry and is only bookkeeping.
(def base-aligned (sol/align base-raw acid-raw [[:C :C] [:CH3 :CH3] [:O :O]]))

;; both states share one origin, so only the proton moves when the pH changes
(def origin (sol/centroid-of acid-raw [:C :O :OXT :CH3]))
(def acid-atoms (sol/recenter acid-raw origin))
(def base-atoms (sol/recenter base-aligned origin))

(def acid-bonds
  [{:a :C   :b :O   :order 2}      ; a real double bond: 1.208 A
   {:a :C   :b :OXT :order 1}      ; a real single bond: 1.342 A
   {:a :C   :b :CH3 :order 1}
   {:a :OXT :b :HXT :order 1}
   {:a :CH3 :b :H1  :order 1}
   {:a :CH3 :b :H2  :order 1}
   {:a :CH3 :b :H3  :order 1}])

;; The CCD has to write acetate as a Kekule structure, so its file calls one
;; C-O double and the other single -- while its own coordinates make them
;; exactly the same length. We draw both as one-and-a-half bonds, because that
;; is what the geometry says and the bond-order column is a notation limit.
(def base-bonds
  [{:a :C   :b :O   :order 1.5}
   {:a :C   :b :OXT :order 1.5}
   {:a :C   :b :CH3 :order 1}
   {:a :CH3 :b :H1  :order 1}
   {:a :CH3 :b :H2  :order 1}
   {:a :CH3 :b :H3  :order 1}])

;; measured back off the coordinates above, not quoted
(defn co-lengths [atoms]
  [(v/len (v/sub (get atoms :O) (get atoms :C)))
   (v/len (v/sub (get atoms :OXT) (get atoms :C)))])

;; ---------------------------------------------------------------------
;; pKa 4.76 for acetic acid. Cell pH is around 7.2 -- more than two units
;; above it -- so essentially every carboxyl group in a cell is sitting in the
;; ionised, negatively charged form. "Carboxyl" in a biology textbook almost
;; always means carboxylATE.
;; ---------------------------------------------------------------------

(def PKA 4.76)
(def pkas [PKA])

(def acid
  (sol/hydrate
    {:label "acetic acid (CH3COOH)"
     :atoms acid-atoms :bonds acid-bonds :charge 0
     ;; the C=O oxygen accepts on two sp2 lone pairs; the -OH oxygen is sp3
     :acceptors [{:atom :O :site :carbonyl} {:atom :OXT :site :hydroxyl}]
     ;; and the one acidic hydrogen donates
     :donors [{:h :HXT :heavy :OXT :site :hydroxyl}]}
    {:seed 20260903
     :hydrophobic [{:atom :CH3 :site :methyl :n 2}]}))

(def base
  (sol/hydrate
    {:label "acetate (CH3COO-)"
     :atoms base-atoms :bonds base-bonds :charge -1
     ;; both oxygens are now equivalent, and both accept
     :acceptors [{:atom :O :site :carboxylate} {:atom :OXT :site :carboxylate}]
     ;; nothing left to give
     :donors []
     ;; the charge is delocalised across both oxygens, so it is drawn on both
     :charges {:O -0.5 :OXT -0.5}}
    {:seed 20260904
     :hydrophobic [{:atom :CH3 :site :methyl :n 2}]}))

(def states [acid base])
