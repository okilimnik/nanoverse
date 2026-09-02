(ns nanoverse.methyl.geometry
  (:require [nanoverse.solvent :as sol]))

;; Pure chemistry for the methyl slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; Alanine, PDB Chemical Component Dictionary entry ALA, ideal coordinates,
;; kept verbatim in structures/ALA.cif. Angstroms.
;;
;; The last slide in the deck, and the only one whose functional group does
;; nothing. A methyl group is a carbon with three hydrogens. It has no lone
;; pair to accept a hydrogen bond with and no polar hydrogen to donate one
;; with, it does not ionise at any pH, and it never carries a charge. Every
;; readout that has meant something on the previous slides sits at zero for
;; it, permanently.
;;
;; That is worth a whole slide because the *absence* is the mechanism. Water
;; cannot hydrogen bond to a methyl, so the waters next to one have to satisfy
;; their bonds among themselves, in a more ordered arrangement than bulk water
;; would take. Ordering costs entropy, and the cheapest way for the system to
;; pay less of it is to shove the greasy surfaces together, out of the water.
;; That shove is the hydrophobic effect: what folds a protein's core, what
;; holds a membrane together, what makes oil separate from vinegar.
;;
;; Alanine also happens to carry a carboxyl AND an amino group, so the two pH
;; slides from earlier in the deck reappear here on one molecule -- and the
;; methyl sits between them ignoring the slider completely.
;; ---------------------------------------------------------------------

(def raw
  {:N   {:e :N :x  -0.9660 :y   0.4930 :z   1.5000}   ; amino group
   :CA  {:e :C :x   0.2570 :y   0.4180 :z   0.6920}   ; the alpha carbon
   :C   {:e :C :x  -0.0940 :y   0.0170 :z  -0.7160}   ; carboxyl carbon
   :O   {:e :O :x  -1.0560 :y  -0.6820 :z  -0.9230}
   :CB  {:e :C :x   1.2040 :y  -0.6200 :z   1.2960}   ; <- THE SIDE CHAIN. A methyl.
   :OXT {:e :O :x   0.6610 :y   0.4390 :z  -1.7420}
   :H   {:e :H :x  -1.3830 :y  -0.4250 :z   1.4820}   ; polar (on N)
   :H2  {:e :H :x  -0.6760 :y   0.6610 :z   2.4520}   ; polar (on N)
   :HA  {:e :H :x   0.7460 :y   1.3920 :z   0.6820}   ; on carbon: inert
   :HB1 {:e :H :x   1.4590 :y  -0.3300 :z   2.3160}   ; on carbon: inert
   :HB2 {:e :H :x   0.7150 :y  -1.5940 :z   1.3070}   ; on carbon: inert
   :HB3 {:e :H :x   2.1130 :y  -0.6760 :z   0.6970}   ; on carbon: inert
   :HXT {:e :H :x   0.4350 :y   0.1820 :z  -2.6470}}) ; <- acidic

(def bonds
  [{:a :N   :b :CA  :order 1}
   {:a :N   :b :H   :order 1}
   {:a :N   :b :H2  :order 1}
   {:a :CA  :b :C   :order 1}
   {:a :CA  :b :CB  :order 1}
   {:a :CA  :b :HA  :order 1}
   {:a :C   :b :O   :order 2}
   {:a :C   :b :OXT :order 1}
   {:a :CB  :b :HB1 :order 1}
   {:a :CB  :b :HB2 :order 1}
   {:a :CB  :b :HB3 :order 1}
   {:a :OXT :b :HXT :order 1}])

(def origin (sol/centroid-of raw [:N :CA :C :O :CB :OXT]))

;; ---------------------------------------------------------------------
;; The CCD entry is the neutral form -- an -NH2 and a -COOH. That form barely
;; exists in water at any pH: below pH 2.34 the amine has already taken a
;; proton, above it the acid has already given one up, and in between BOTH
;; have happened at once. An amino acid in water is a zwitterion, positive at
;; one end and negative at the other with a net charge of zero.
;;
;; So the fully protonated species -- the cation, the one at the bottom of the
;; ladder -- is built by ADDING the third N-H (constructed; see
;; solvent/protonate), and every rung above it is an exact removal.
;; ---------------------------------------------------------------------

(def PKA-COOH 2.34)   ; alpha-carboxyl
(def PKA-NH3 9.69)    ; alpha-amino
(def pkas [PKA-COOH PKA-NH3])

(def cation-base
  (sol/protonate {:atoms (sol/recenter raw origin) :bonds bonds} :N :H3))

(def all-acceptors
  ;; The nitrogen is listed at every rung on purpose. When it is an -NH3+ it
  ;; has four neighbours, solvent/lone-pair-dirs finds no free pair on it, and
  ;; it silently stops accepting -- the structure decides, not a special case.
  [{:atom :N   :site :amino}
   {:atom :O   :site :carboxyl}
   {:atom :OXT :site :carboxyl}])

(def all-donors
  [{:h :H   :heavy :N   :site :amino}
   {:h :H2  :heavy :N   :site :amino}
   {:h :H3  :heavy :N   :site :amino}
   {:h :HXT :heavy :OXT :site :carboxyl}])

(def charges-by-step
  [{:N 1}                             ; cation: NH3+ / COOH
   {:N 1 :O -0.5 :OXT -0.5}           ; zwitterion: NH3+ / COO-
   {:O -0.5 :OXT -0.5}])              ; anion: NH2 / COO-

(def labels
  ["alanine cation (NH3+ / COOH)"
   "alanine zwitterion (NH3+ / COO−)"
   "alanine anion (NH2 / COO−)"])

;; the protons come off in ascending pKa order: the carboxyl first, the
;; ammonium second
(def released-order [:HXT :H3])

(defn- state-at [n]
  (let [st (reduce sol/deprotonate cation-base (take n released-order))
        present (:atoms st)
        ;; once HXT is gone the carboxylate's two C-O bonds are equivalent
        bonds' (if (>= n 1)
                 (mapv (fn [b]
                         (if (and (= (:a b) :C) (or (= (:b b) :O) (= (:b b) :OXT)))
                           (assoc b :order 1.5)
                           b))
                       (:bonds st))
                 (:bonds st))]
    (sol/hydrate
      (merge st
             {:bonds bonds'
              :label (nth labels n)
              :charge (- 1 n)
              :acceptors all-acceptors
              :donors (vec (filter #(some? (get present (:h %))) all-donors))
              :charges (nth charges-by-step n)})
      {:seed (+ 20260910 n)
       ;; the side chain, declared as what it is. Four waters are parked
       ;; against it in every state so there is always something there to
       ;; watch not happening.
       :hydrophobic [{:atom :CB :site :methyl :n 4}]})))

(def states (mapv state-at [0 1 2]))

;; number of waters sitting on the methyl -- the same in every state, because
;; nothing about the methyl depends on pH
(def methyl-water-count
  (count (filter #(= (:site %) :methyl) (:waters (nth states 1)))))

(def CELL-PH 7.4)
