(ns nanoverse.phosphate.geometry
  (:require [nanoverse.solvent :as sol]))

;; Pure chemistry for the phosphate slide -- no rendering-engine dependency.
;;
;; ---------------------------------------------------------------------
;; 3-phosphoglyceric acid, PDB Chemical Component Dictionary entry 3PG, ideal
;; coordinates, kept verbatim in structures/3PG.cif. Angstroms.
;;
;; This is a real metabolite, not a demonstration molecule: it is the product
;; of the first ATP-generating step of glycolysis, and the substrate carbon
;; that ends up in every sugar a plant makes. It was chosen here because it
;; carries THREE ionisable protons with pKas spread across the useful range,
;; plus one hydroxyl that never ionises at all -- a built-in control.
;;
;; The CCD entry is the fully protonated ACID. Every other state on this slide
;; is that structure with a hydrogen removed, which is an exact operation --
;; nothing is invented, an atom is dropped.
;; ---------------------------------------------------------------------

(def raw
  {:C1   {:e :C :x  -0.3020 :y   0.0060 :z  -2.8520}   ; carboxyl carbon
   :O1   {:e :O :x   0.2850 :y  -0.6330 :z  -3.6920}   ; C=O
   :O2   {:e :O :x  -1.6250 :y   0.2020 :z  -2.9640}   ; C-O-H
   :C2   {:e :C :x   0.4480 :y   0.5820 :z  -1.6800}
   :O3   {:e :O :x   1.8310 :y   0.2390 :z  -1.7850}   ; plain alcohol -- never ionises
   :C3   {:e :C :x  -0.1210 :y   0.0120 :z  -0.3790}
   :O1P  {:e :O :x   0.5900 :y   0.5590 :z   0.7310}   ; the ester oxygen, C-O-P
   :P    {:e :P :x  -0.0570 :y  -0.0790 :z   2.0590}
   :O2P  {:e :O :x   0.0830 :y  -1.5520 :z   2.0180}   ; P=O, 1.480 A
   :O3P  {:e :O :x   0.7010 :y   0.4950 :z   3.3570}   ; P-O-H, 1.609 A
   :O4P  {:e :O :x  -1.6180 :y   0.3070 :z   2.1340}   ; P-O-H, 1.610 A
   :HO2  {:e :H :x  -2.1070 :y  -0.1670 :z  -3.7170}   ; <- acidic (carboxyl)
   :H2   {:e :H :x   0.3420 :y   1.6670 :z  -1.6790}
   :HO3  {:e :H :x   1.8800 :y  -0.7260 :z  -1.7830}   ; <- NOT acidic (alcohol)
   :H31  {:e :H :x  -1.1760 :y   0.2740 :z  -0.2990}
   :H32  {:e :H :x  -0.0150 :y  -1.0720 :z  -0.3810}
   :HOP3 {:e :H :x   0.2830 :y   0.0880 :z   4.1290}   ; <- acidic (phosphate 1)
   :HOP4 {:e :H :x  -1.6660 :y   1.2730 :z   2.1600}}) ; <- acidic (phosphate 2)

(def bonds
  [{:a :C1  :b :O1   :order 2}
   {:a :C1  :b :O2   :order 1}
   {:a :C1  :b :C2   :order 1}
   {:a :O2  :b :HO2  :order 1}
   {:a :C2  :b :O3   :order 1}
   {:a :C2  :b :C3   :order 1}
   {:a :C2  :b :H2   :order 1}
   {:a :O3  :b :HO3  :order 1}
   {:a :C3  :b :O1P  :order 1}
   {:a :C3  :b :H31  :order 1}
   {:a :C3  :b :H32  :order 1}
   {:a :O1P :b :P    :order 1}
   {:a :P   :b :O2P  :order 2}
   {:a :P   :b :O3P  :order 1}
   {:a :P   :b :O4P  :order 1}
   {:a :O3P :b :HOP3 :order 1}
   {:a :O4P :b :HOP4 :order 1}])

(def heavy [:C1 :O1 :O2 :C2 :O3 :C3 :O1P :P :O2P :O3P :O4P])
(def atoms (sol/recenter raw (sol/centroid-of raw heavy)))

;; ---------------------------------------------------------------------
;; The titration ladder, in ascending pKa order -- which is also the order the
;; protons actually come off as you raise the pH.
;;
;; These are APPROXIMATE literature values for a phosphate monoester carrying
;; an alpha-hydroxy acid. Real measured values shift by a few tenths with
;; ionic strength, temperature and the counter-ion, and sources disagree at
;; that level. The shape of the ladder is what matters here, not the third
;; decimal place.
;;
;; The fourth oxygen-hydrogen on the molecule, HO3, is a plain secondary
;; alcohol. Its pKa is around 15 -- off the top of the slider -- so it is
;; still there at every pH you can reach. Three protons leave, one never does,
;; and they all look identical in the structure file. That is the point of
;; having it here.
;; ---------------------------------------------------------------------

(def ladder
  [{:pka 1.42 :h :HOP3 :heavy :O3P :group "1st phosphate"}
   {:pka 3.42 :h :HO2  :heavy :O2  :group "carboxyl"}
   {:pka 6.21 :h :HOP4 :heavy :O4P :group "2nd phosphate"}])

(def pkas (mapv :pka ladder))

;; Every acceptor and every potential donor the molecule has. Which donors are
;; actually available is then decided by which hydrogens still exist in a
;; given state -- so the behaviour follows the structure instead of being
;; declared four times over.
(def all-acceptors
  [{:atom :O1  :site :carboxyl}
   {:atom :O2  :site :carboxyl}
   {:atom :O3  :site :hydroxyl}
   {:atom :O2P :site :phosphate}
   {:atom :O3P :site :phosphate}
   {:atom :O4P :site :phosphate}])

(def all-donors
  [{:h :HO2 :heavy :O2 :site :carboxyl}
   {:h :HO3 :heavy :O3 :site :hydroxyl}
   {:h :HOP3 :heavy :O3P :site :phosphate}
   {:h :HOP4 :heavy :O4P :site :phosphate}])

;; Formal charges to draw, per number of protons released. The phosphate's
;; charge is genuinely delocalised over its free oxygens and the carboxylate's
;; over its two, so it is drawn spread rather than parked on one atom.
(def charges-by-step
  [{}
   {:O3P -1}
   {:O3P -1 :O1 -0.5 :O2 -0.5}
   {:O2P -0.67 :O3P -0.67 :O4P -0.67 :O1 -0.5 :O2 -0.5}])

(defn- state-at
  "The molecule with the first `n` protons of the ladder removed."
  [n]
  (let [dropped (mapv :h (take n ladder))
        st (reduce sol/deprotonate {:atoms atoms :bonds bonds} dropped)
        present (:atoms st)
        ;; carboxylate: once HO2 is gone the two C-O bonds are equivalent.
        ;; (The coordinates are still the acid's -- see the slide's LIMITS.)
        bonds' (if (>= n 2)
                 (mapv (fn [b]
                         (if (and (= (:a b) :C1) (or (= (:b b) :O1) (= (:b b) :O2)))
                           (assoc b :order 1.5)
                           b))
                       (:bonds st))
                 (:bonds st))]
    (sol/hydrate
      (merge st
             {:bonds bonds'
              :label (str "3-phosphoglycerate " (sol/signed-charge (- n)))
              :charge (- n)
              :released (mapv :group (take n ladder))
              :acceptors all-acceptors
              :donors (vec (filter #(some? (get present (:h %))) all-donors))
              :charges (nth charges-by-step n)})
      ;; no hydrophobic sites declared: this molecule has almost no greasy
      ;; surface left to show, which is itself the contrast with the methyl
      ;; slide at the end of the deck
      {:seed (+ 20260907 n)})))

(def states (mapv state-at [0 1 2 3]))

;; Cytosolic pH. Quoted here because the whole slide is an argument about
;; where this molecule sits relative to it.
(def CELL-PH 7.2)
