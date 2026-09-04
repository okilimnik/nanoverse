(ns nanoverse.aminoacid.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]
            [nanoverse.aminoacid.residues :as res]))

;; Pure chemistry for the amino-acid chapter -- no rendering-engine dependency.
;;
;; The coordinates live next door in nanoverse.aminoacid.residues, generated
;; straight from the PDB Chemical Component Dictionary. This file is the part
;; that is a chemical CLAIM rather than a measurement: for each side chain,
;; which atom accepts a hydrogen bond, which hydrogen donates one, which
;; surface is greasy, and at what pH the group changes hands. Those are the
;; things worth arguing with, so they are all in one readable table.
;;
;; ---------------------------------------------------------------------
;; The experiment the whole chapter runs
;;
;; Twenty-one residues, ONE backbone, in ONE pose. Every residue is rigidly
;; aligned onto alanine's N-CA-C frame, so switching residues moves nothing
;; except the side chain -- the thing under study is the only thing that
;; changes. And every residue is drawn as the ZWITTERION, the form that
;; actually exists in water: -NH3+ at one end, -COO- at the other, net zero.
;;
;; Then the deliberate distortion, which is the price of the experiment:
;; ONLY THE SIDE CHAIN IS HYDRATED. No water is placed on the backbone, even
;; though a real zwitterionic backbone is the most heavily hydrated part of a
;; free amino acid by some distance. That is stated on every slide's face,
;; and it buys something worth having: the number on screen is a property of
;; the side chain alone, and it is comparable from one residue to the next.
;; A residue with no polar side-chain site declares no candidate pairs at
;; all, so its zero is zero BY CONSTRUCTION rather than by a threshold that
;; happens never to be met.
;; ---------------------------------------------------------------------

(def BACKBONE [:N :CA :C :O :OXT])

;; Alanine's backbone, centred on itself, is the frame every other residue is
;; posed against. Nothing special about alanine except that it is the smallest
;; residue with a side chain at all.
(def ref-atoms
  (let [a (:atoms res/ALA)]
    (sol/recenter a (sol/centroid-of a BACKBONE))))

(defn- posed
  "Rigidly drop a residue onto the shared backbone frame. Exact three-point
   match on N-CA-C, which every one of the twenty-one has."
  [r]
  (sol/align (:atoms r) ref-atoms [[:CA :CA] [:N :N] [:C :C]]))

(defn- equalise
  "Redraw named bonds as one-and-a-half. Used where a group's charge is shared
   across two bonds that the file, having to pick a Kekule structure, wrote as
   one double and one single."
  [state pairs]
  (assoc state :bonds
         (mapv (fn [b]
                 (if (some (fn [[x y]] (or (and (= (:a b) x) (= (:b b) y))
                                           (and (= (:a b) y) (= (:b b) x))))
                           pairs)
                   (assoc b :order 1.5)
                   b))
               (:bonds state))))

;; ---------------------------------------------------------------------
;; Unfolding the side chain
;;
;; THIS IS THE ONE PIECE OF GEOMETRY IN THE CHAPTER THAT IS NOT READ OUT OF A
;; FILE, so it gets the longest comment.
;;
;; A CCD "ideal" conformer is one arbitrary low-energy pose, and a lot of them
;; are curled: the side chain folded back until it lies against its own
;; backbone. Asparagine's amide oxygen ends up 2.8 A from the backbone
;; carboxylate that way, with its lone pair pointing straight at it -- so
;; there is nowhere to put the water that should be hydrogen bonding to it,
;; and the slide would report asparagine accepting NOTHING. That would be a
;; flat falsehood about a residue whose whole job is to accept, caused
;; entirely by which rotamer somebody happened to deposit.
;;
;; So each freely rotating side-chain bond is turned, working outward from the
;; alpha carbon, to whichever STAGGERED position puts the side chain furthest
;; from its own backbone.
;;
;; What that costs, precisely:
;;   * nothing but torsions change. Every bond length and every bond angle is
;;     still the deposited one.
;;   * only staggered angles are offered (-60, +60, 180). An eclipsed
;;     conformer is never produced, because real molecules do not sit in one.
;;   * a bond inside a ring is not turned at all -- solvent/branch-keys sees
;;     that the walk comes back on itself and declines. That is why proline
;;     and the aromatic rings come through untouched.
;;   * it is greedy and innermost-first, so it finds an extended conformer,
;;     not the global optimum of anything. It is not an energy minimisation
;;     and there is no force field behind it.
;; ---------------------------------------------------------------------

(def ^:private STAGGERED [-60 60 180])

(defn- torsion-chain
  "The freely rotating bonds of a side chain, innermost first, each written as
   the four atoms whose torsion turns it. Walks heavy atoms outward from the
   alpha carbon; stops when it runs out of side chain."
  [state]
  (let [atoms (:atoms state)
        bonds (:bonds state)
        heavy-nbrs (fn [k] (filter #(and (get atoms %) (not= (:e (get atoms %)) :H))
                                   (sol/neighbours bonds k)))]
    (loop [acc [] prev :N a :CA seen {:N true :C true :O true :OXT true :CA true}]
      (let [b (first (remove #(get seen %) (heavy-nbrs a)))]
        (if (nil? b)
          acc
          (let [seen' (assoc seen b true)
                c (first (remove #(get seen' %) (heavy-nbrs b)))]
            (if (nil? c)
              acc
              (recur (conj acc [prev a b c]) a b seen'))))))))

(defn- far-pairs
  "Heavy-atom pairs three or more bonds apart. Anything closer than that --
   a bond, or two atoms sharing a neighbour -- is held at a fixed distance by
   the bond lengths and angles, so no torsion can move it and there is no
   point scoring it."
  [state]
  (let [atoms (:atoms state)
        bonds (:bonds state)
        heavy (filter #(not= (:e (get atoms %)) :H) (keys atoms))
        near (into {} (for [k heavy]
                        [k (into {} (for [n (concat [k]
                                                    (sol/neighbours bonds k)
                                                    (mapcat #(sol/neighbours bonds %)
                                                            (sol/neighbours bonds k)))]
                                      [n true]))]))]
    (vec (for [[i a] (map-indexed vector heavy)
               [j b] (map-indexed vector heavy)
               :when (and (< i j) (not (get (get near a) b)))]
           [a b]))))

(defn- crowding
  "The closest approach between two heavy atoms that a torsion could actually
   separate. Maximising it is exactly what 'unfolded' means here: nothing --
   backbone or side chain -- is folded back on top of anything else."
  [atoms pairs]
  (reduce min 99 (map (fn [[a b]] (v/dist (get atoms a) (get atoms b))) pairs)))

(defn- unfold
  "Turn each rotatable side-chain bond, innermost first, to the staggered
   position that leaves the molecule least folded onto itself."
  [state]
  (let [pairs (far-pairs state)]
    (reduce (fn [st t]
              (let [best (reduce (fn [acc angle]
                                   (let [c (sol/set-torsion st t angle)
                                         score (crowding (:atoms c) pairs)]
                                     (if (or (nil? acc) (> score (:score acc)))
                                       {:score score :state c}
                                       acc)))
                                 nil
                                 STAGGERED)]
                (:state best)))
            state
            (torsion-chain state))))

;; -NH3+ and -COO-, on every residue, at every pH this chapter shows. The
;; backbone alpha-carboxyl (pKa ~2.2) and alpha-amino (pKa ~9.5) are outside
;; the range where the SIDE CHAINS are interesting, so they are held fixed;
;; the functional-group chapter's methyl slide is where the backbone itself
;; titrates.
(def BACKBONE-CHARGES {:N 1 :O -0.5 :OXT -0.5})

(defn- zwitterion
  "The CCD entry, posed, with its alpha-amino protonated and its alpha-carboxyl
   deprotonated.

   Note the asymmetry, which is the usual one in this deck: taking HXT off is
   EXACT -- an atom is simply dropped -- while the third N-H is CONSTRUCTED at
   the one tetrahedral vertex the other three bonds leave open, and nothing is
   re-relaxed afterwards. Proline is the interesting case: its nitrogen starts
   with only one hydrogen because the side chain occupies the other position,
   so the same call leaves it with two, not three."
  [r]
  (-> {:atoms (posed r) :bonds (:bonds r)}
      (sol/protonate :N :HN3)
      (sol/deprotonate :HXT)
      (equalise [[:C :O] [:C :OXT]])
      unfold))

;; ---------------------------------------------------------------------
;; The table
;;
;; :forms is an ordered protonation ladder, MOST protonated first, exactly as
;; the deck's titration machinery expects. A residue with one form has nothing
;; that ionises in the range the slider covers -- or, for arginine, nothing
;; that can be drawn without inventing a tautomer.
;;
;; :drop lists hydrogens removed from the deposited structure (exact).
;; :add lists hydrogens constructed onto it (stated as such on the slide).
;; :phobic sites get waters at van der Waals contact and can never bond.
;;
;; pKa values are free-amino-acid side-chain values in dilute water at 25 C.
;; Hydropathy is Kyte & Doolittle (1982), tabulated, not computed here.
;; ---------------------------------------------------------------------

(def ^:private acc (fn [k] {:atom k :site :side}))
(def ^:private don (fn [h heavy] {:h h :heavy heavy :site :side}))

(def RESIDUES
  [;; ---- electrically charged, negative -------------------------------
   {:code :ASP :one "D" :three "Asp" :name "Aspartate"
    :class :charged :sign :negative
    :side [:CB :CG :OD1 :OD2]
    :pka 3.65 :acid? true :hydropathy -3.5
    :blurb "The shortest acidic side chain there is: one CH₂ and a carboxyl.
            Above pKa 3.65 it is a carboxylate, which in a cell means always."
    :forms [{:label "aspartic acid (–COOH)" :charge 0
             :acceptors [(acc :OD1) (acc :OD2)]
             :donors [(don :HD2 :OD2)]}
            {:label "aspartate (–COO⁻)" :charge -1 :drop [:HD2]
             :equalise [[:CG :OD1] [:CG :OD2]]
             :charges {:OD1 -0.5 :OD2 -0.5}
             :acceptors [(acc :OD1) (acc :OD2)]
             :donors []}]}

   {:code :GLU :one "E" :three "Glu" :name "Glutamate"
    :class :charged :sign :negative
    :side [:CB :CG :CD :OE1 :OE2]
    :pka 4.25 :acid? true :hydropathy -3.5
    :blurb "Aspartate plus one CH₂. The extra carbon moves the charge half an
            ångström further from the backbone and raises the pKa by 0.6 —
            the same group, held further out, holds its proton a little harder."
    :forms [{:label "glutamic acid (–COOH)" :charge 0
             :acceptors [(acc :OE1) (acc :OE2)]
             :donors [(don :HE2 :OE2)]}
            {:label "glutamate (–COO⁻)" :charge -1 :drop [:HE2]
             :equalise [[:CD :OE1] [:CD :OE2]]
             :charges {:OE1 -0.5 :OE2 -0.5}
             :acceptors [(acc :OE1) (acc :OE2)]
             :donors []}]}

   ;; ---- electrically charged, positive -------------------------------
   {:code :LYS :one "K" :three "Lys" :name "Lysine"
    :class :charged :sign :positive
    :side [:CB :CG :CD :CE :NZ]
    :pka 10.53 :acid? false :hydropathy -3.9
    :blurb "Four greasy CH₂ groups with an ammonium on the end — a charge held
            out on a stalk. Exactly the amine from the functional-group
            chapter, and it does the same trade: it gives three hydrogen bonds
            and, having spent its lone pair on a proton, accepts none."
    :forms [{:label "lysine, –NH₃⁺" :charge 1
             :charges {:NZ 1}
             :acceptors []
             :donors [(don :HZ1 :NZ) (don :HZ2 :NZ) (don :HZ3 :NZ)]}
            {:label "lysine, neutral –NH₂" :charge 0 :drop [:HZ3]
             :acceptors [(acc :NZ)]
             :donors [(don :HZ1 :NZ) (don :HZ2 :NZ)]}]}

   {:code :ARG :one "R" :three "Arg" :name "Arginine"
    :class :charged :sign :positive
    :side [:CB :CG :CD :NE :CZ :NH1 :NH2]
    :pka 12.48 :acid? false :hydropathy -4.5
    :blurb "A guanidinium group: one carbon, three nitrogens, one positive
            charge spread flat across all of them. Five N–H bonds pointing
            outward make it the best hydrogen-bond donor in the set, and the
            highest pKa — the most reliably positive thing in a protein."
    :forms [{:label "arginine, guanidinium (+1)" :charge 1
             ;; the + is delocalised over the three nitrogens, which is why
             ;; the group is planar and why its pKa is so high
             :charges {:NE 0.33 :NH1 0.33 :NH2 0.33}
             :acceptors []
             :donors [(don :HE :NE) (don :HH11 :NH1) (don :HH12 :NH1)
                      (don :HH21 :NH2) (don :HH22 :NH2)]}]}

   {:code :HIS :one "H" :three "His" :name "Histidine"
    :class :charged :sign :positive
    :side [:CB :CG :ND1 :CD2 :CE1 :NE2]
    :pka 6.00 :acid? false :hydropathy -3.2
    :blurb "The only side chain whose pKa sits near cell pH — so it is the only
            one that is genuinely part-charged in the cell, and the only one
            that can pick a proton up and put it down again during a reaction.
            Most enzymes that move a proton around use a histidine to do it."
    :forms [{:label "histidine, imidazolium (+1)" :charge 1
             :charges {:ND1 0.5 :NE2 0.5}
             :acceptors []
             :donors [(don :HD1 :ND1) (don :HE2 :NE2)]}
            {:label "histidine, neutral (Nδ1–H tautomer)" :charge 0 :drop [:HE2]
             ;; NE2 now has two ring neighbours and no hydrogen. Because both
             ;; of its bonds are aromatic, solvent/lone-pair-dirs gives it ONE
             ;; lone pair in the ring plane, which is what a pyridine-type
             ;; nitrogen actually has.
             :acceptors [(acc :NE2)]
             :donors [(don :HD1 :ND1)]}]}

   ;; ---- polar, uncharged ---------------------------------------------
   {:code :SER :one "S" :three "Ser" :name "Serine"
    :class :polar
    :side [:CB :OG]
    :hydropathy -0.8
    :blurb "A hydroxyl on a one-carbon stalk — the ethanol slide, bolted to a
            backbone. It gives one hydrogen bond and takes two, and there is no
            pH on any slider in this deck that charges it."
    :forms [{:label "serine (–CH₂OH)" :charge 0
             :acceptors [(acc :OG)]
             :donors [(don :HG :OG)]}]}

   {:code :THR :one "T" :three "Thr" :name "Threonine"
    :class :polar
    :side [:CB :OG1 :CG2]
    :hydropathy -0.7
    :blurb "Serine plus a methyl, on the same carbon. Polar and greasy at once:
            the –OH keeps hydrogen bonding while the –CH₃ sits next to it doing
            nothing at all. Watch the two kinds of water side by side."
    :forms [{:label "threonine (–CHOH–CH₃)" :charge 0
             :acceptors [(acc :OG1)]
             :donors [(don :HG1 :OG1)]
             :phobic [{:atom :CG2 :site :phobic :n 2}]}]}

   {:code :ASN :one "N" :three "Asn" :name "Asparagine"
    :class :polar
    :side [:CB :CG :OD1 :ND2]
    :hydropathy -3.5
    :blurb "Aspartate's amide — the same carbon skeleton with an –NH₂ where the
            second oxygen was, and no charge at any pH. Its oxygen takes and
            its two N–H bonds give, so it does both jobs at once from opposite
            ends of one small group."
    :forms [{:label "asparagine (–CONH₂)" :charge 0
             ;; ND2 is deliberately NOT an acceptor: an amide nitrogen's lone
             ;; pair is delocalised into the C=O and is not available.
             :acceptors [(acc :OD1)]
             :donors [(don :HD21 :ND2) (don :HD22 :ND2)]}]}

   {:code :GLN :one "Q" :three "Gln" :name "Glutamine"
    :class :polar
    :side [:CB :CG :CD :OE1 :NE2]
    :hydropathy -3.5
    :blurb "Asparagine plus a CH₂, exactly as glutamate is aspartate plus a
            CH₂. Same chemistry, longer reach — which is the whole reason
            evolution keeps both members of each pair around."
    :forms [{:label "glutamine (–CONH₂)" :charge 0
             :acceptors [(acc :OE1)]
             :donors [(don :HE21 :NE2) (don :HE22 :NE2)]}]}

   ;; ---- special cases -------------------------------------------------
   {:code :GLY :one "G" :three "Gly" :name "Glycine"
    :class :special
    :side []
    :hydropathy -0.4
    :blurb "No side chain at all — the fourth bond on the alpha carbon is a
            hydrogen. That makes it the only achiral amino acid, and by far the
            most flexible: with nothing in the way, the backbone can fold into
            angles no other residue can reach. Collagen is one in three glycine."
    :forms [{:label "glycine (side chain = one H)" :charge 0
             :acceptors []
             :donors []
             ;; the alpha hydrogens, treated as the vanishingly small greasy
             ;; surface they are, so there is something on screen to watch
             ;; not happening
             :phobic [{:atom :CA :site :phobic :n 2}]}]}

   {:code :PRO :one "P" :three "Pro" :name "Proline"
    :class :special
    :side [:CB :CG :CD]
    :hydropathy -1.6
    :blurb "The side chain loops back and bonds to its own backbone nitrogen.
            That ring is a straitjacket: the backbone angle at proline is
            locked, and in a peptide the nitrogen has NO hydrogen left to give.
            Proline is where an alpha helix breaks."
    :forms [{:label "proline (side chain closes onto N)" :charge 0
             :acceptors []
             :donors []
             :phobic [{:atom :CG :site :phobic :n 2}]}]}

   {:code :CYS :one "C" :three "Cys" :name "Cysteine"
    :class :special
    :side [:CB :SG]
    :pka 8.18 :acid? true :hydropathy 2.5
    :blurb "Serine with the oxygen swapped for the sulfur below it. Two things
            follow: its hydrogen bonds are feeble, and it can form the S–S
            bridge — the one covalent bond a cell makes and breaks inside a
            folded protein. Its pKa of 8.18 is close enough to cell pH that the
            reactive thiolate is always available in small amounts."
    :forms [{:label "cysteine, thiol (–SH)" :charge 0
             :acceptors [(acc :SG)]
             :donors [(don :HG :SG)]}
            {:label "cysteinate, thiolate (–S⁻)" :charge -1 :drop [:HG]
             :charges {:SG -1}
             ;; one neighbour, and it is a plain single bond to an sp3 carbon,
             ;; so the pairs fan out on a cone rather than lying in a plane
             :acceptors [{:atom :SG :site :side :max-lp 3}]
             :donors []}]}

   {:code :SEC :one "U" :three "Sec" :name "Selenocysteine"
    :class :special
    :side [:CB :SE]
    :pka 5.24 :acid? true :hydropathy nil
    :blurb "The 21st amino acid, encoded by a UGA codon that would otherwise
            stop translation. Cysteine with selenium one row further down: a
            bigger, softer atom with a pKa of 5.24, which means that unlike
            cysteine it is fully ionised at cell pH. Always ready, always
            reactive — which is why it sits in the active site of the enzymes
            that mop up peroxide."
    :forms [{:label "selenocysteine, selenol (–SeH)" :charge 0
             :acceptors [(acc :SE)]
             :donors [(don :HE :SE)]}
            {:label "selenolate (–Se⁻)" :charge -1 :drop [:HE]
             :charges {:SE -1}
             :acceptors [{:atom :SE :site :side :max-lp 3}]
             :donors []}]}

   ;; ---- nonpolar, hydrophobic ------------------------------------------
   {:code :ALA :one "A" :three "Ala" :name "Alanine"
    :class :nonpolar
    :side [:CB]
    :hydropathy 1.8
    :blurb "One methyl: the smallest hydrophobic side chain. Nothing to give,
            nothing to take, no pH that changes it. The functional-group
            chapter ends on this molecule for exactly that reason."
    :forms [{:label "alanine (–CH₃)" :charge 0
             :acceptors [] :donors []
             :phobic [{:atom :CB :site :phobic :n 3}]}]}

   {:code :VAL :one "V" :three "Val" :name "Valine"
    :class :nonpolar
    :side [:CB :CG1 :CG2]
    :hydropathy 4.2
    :blurb "Two methyls branching off one carbon. Branched right at the base,
            which makes it bulky close in — valine is one of the residues that
            most favours a beta sheet over a helix."
    :forms [{:label "valine (–CH(CH₃)₂)" :charge 0
             :acceptors [] :donors []
             :phobic [{:atom :CG1 :site :phobic :n 2} {:atom :CG2 :site :phobic :n 2}]}]}

   {:code :LEU :one "L" :three "Leu" :name "Leucine"
    :class :nonpolar
    :side [:CB :CG :CD1 :CD2]
    :hydropathy 3.8
    :blurb "Valine's two methyls, moved one carbon further out. The most common
            amino acid in most proteomes, and the workhorse of the hydrophobic
            core — the leucine zipper is named after a run of them."
    :forms [{:label "leucine (–CH₂CH(CH₃)₂)" :charge 0
             :acceptors [] :donors []
             :phobic [{:atom :CD1 :site :phobic :n 2} {:atom :CD2 :site :phobic :n 2}]}]}

   {:code :ILE :one "I" :three "Ile" :name "Isoleucine"
    :class :nonpolar
    :side [:CB :CG1 :CG2 :CD1]
    :hydropathy 4.5
    :blurb "The same four carbons as leucine, wired differently — and the most
            hydrophobic residue on the Kyte–Doolittle scale. Isoleucine has a
            second chiral centre, the only residue besides threonine that does."
    :forms [{:label "isoleucine (–CH(CH₃)CH₂CH₃)" :charge 0
             :acceptors [] :donors []
             :phobic [{:atom :CD1 :site :phobic :n 2} {:atom :CG2 :site :phobic :n 2}]}]}

   {:code :MET :one "M" :three "Met" :name "Methionine"
    :class :nonpolar
    :side [:CB :CG :SD :CE]
    :hydropathy 1.9
    :blurb "A thioether buried in a greasy chain. Classed as nonpolar, and the
            classification is a simplification: that sulfur really does accept
            hydrogen bonds — just very weak ones. Every protein you make starts
            with this residue."
    :forms [{:label "methionine (–CH₂CH₂–S–CH₃)" :charge 0
             ;; a thioether sulfur is a genuine, and genuinely feeble, acceptor
             :acceptors [(acc :SD)]
             :donors []
             :phobic [{:atom :CE :site :phobic :n 2}]}]}

   {:code :PHE :one "F" :three "Phe" :name "Phenylalanine"
    :class :nonpolar
    :side [:CB :CG :CD1 :CD2 :CE1 :CE2 :CZ]
    :hydropathy 2.8
    :blurb "A benzene ring on a stalk: flat, rigid, and completely unable to
            hydrogen bond. Big flat hydrophobic surfaces stack against each
            other in a protein core, which is a different kind of packing from
            what a floppy leucine does."
    :forms [{:label "phenylalanine (–CH₂–C₆H₅)" :charge 0
             :acceptors [] :donors []
             :phobic [{:atom :CZ :site :phobic :n 2}
                      {:atom :CE1 :site :phobic :n 1} {:atom :CE2 :site :phobic :n 1}]}]}

   {:code :TYR :one "Y" :three "Tyr" :name "Tyrosine"
    :class :nonpolar
    :side [:CB :CG :CD1 :CD2 :CE1 :CE2 :CZ :OH]
    :pka 10.07 :acid? true :hydropathy -1.3
    :blurb "Phenylalanine with an –OH on the far edge of the ring. One oxygen,
            and the residue changes group: it hydrogen bonds, it ionises at
            pKa 10.07, and its hydropathy flips from +2.8 to −1.3. It is filed
            under hydrophobic anyway, which tells you how rough the filing is."
    :forms [{:label "tyrosine (–C₆H₄–OH)" :charge 0
             :acceptors [(acc :OH)]
             :donors [(don :HH :OH)]
             :phobic [{:atom :CD1 :site :phobic :n 1} {:atom :CD2 :site :phobic :n 1}]}]}

   {:code :TRP :one "W" :three "Trp" :name "Tryptophan"
    :class :nonpolar
    :side [:CB :CG :CD1 :CD2 :NE1 :CE2 :CE3 :CZ2 :CZ3 :CH2]
    :hydropathy -0.9
    :blurb "The biggest side chain: a two-ring indole with a nitrogen in it.
            That N–H gives a hydrogen bond — but the nitrogen never takes one,
            because its lone pair is spent holding the ring's aromatic system
            together. A donor that cannot accept, which is rare."
    :forms [{:label "tryptophan (indole)" :charge 0
             ;; NE1 is a donor only. A pyrrole-type nitrogen's lone pair is
             ;; part of the aromatic sextet and is not available to a water.
             :acceptors []
             :donors [(don :HE1 :NE1)]
             :phobic [{:atom :CZ2 :site :phobic :n 1} {:atom :CH2 :site :phobic :n 1}
                      {:atom :CZ3 :site :phobic :n 1}]}]}])

;; ---------------------------------------------------------------------
;; Building the states
;; ---------------------------------------------------------------------

(defn- build-form [r form seed]
  (let [base (zwitterion (get res/all (:code r)))
        dropped (reduce sol/deprotonate base (or (:drop form) []))
        eq (equalise dropped (or (:equalise form) []))
        present (:atoms eq)
        side-charge (or (:charge form) 0)]
    (sol/hydrate
      (merge eq
             {:label (:label form)
              ;; backbone is a zwitterion, so net charge is the side chain's
              :charge side-charge
              :acceptors (or (:acceptors form) [])
              :donors (vec (filter #(some? (get present (:h %))) (or (:donors form) [])))
              :charges (merge BACKBONE-CHARGES (or (:charges form) {}))})
      {:seed seed :hydrophobic (or (:phobic form) [])})))

(defn- n-backbone-h
  "How many hydrogens the backbone nitrogen carries, read off the built
   structure rather than looked up. Proline is the residue this exists for."
  [state]
  (count (filter #(= (:e (get (:atoms state) %)) :H)
                 (sol/neighbours (:bonds state) :N))))

(defn- greasy-keys
  "Side-chain carbons and their hydrogens -- what the shading toggle paints.
   Derived from the bond table, so a residue never has to list them twice."
  [r state]
  (let [atoms (:atoms state)
        carbons (filter #(= (:e (get atoms %)) :C) (:side r))]
    (vec (concat carbons
                 (mapcat (fn [k]
                           (filter #(= (:e (get atoms %)) :H)
                                   (sol/neighbours (:bonds state) k)))
                         carbons)))))

(defn- squeeze
  "Collapse the whitespace a multi-line string literal picks up from its own
   indentation. The blurbs are written as prose in the table above and land in
   the DOM through textContent, which would otherwise show the source's line
   breaks."
  [s]
  (.trim (.replace (str s) (js/RegExp. "\\s+" "g") " ")))

(defn build
  "Every residue of one class, each with its protonation ladder already built.
   Seeded per residue and per form so the wobble is reproducible and the
   residues are decorrelated from each other."
  [class-key]
  (vec (for [[i r] (map-indexed vector (filter #(= (:class %) class-key) RESIDUES))]
         (let [states (vec (for [[j f] (map-indexed vector (:forms r))]
                             (build-form r f (+ 20260904 (* 1000 i) (* 37 j)))))]
           (merge r
                  {:blurb (squeeze (:blurb r))
                   :states states
                   :pkas (if (> (count states) 1) [(:pka r)] [])
                   :backbone-h (n-backbone-h (nth states 0))
                   :phobic-waters (count (filter #(= (:site %) :phobic)
                                                 (:waters (nth states 0))))
                   :greasy (greasy-keys r (nth states 0))})))))

(defn view
  "Where to point the camera so that every residue of a class fits on screen,
   waters and all.

   ONE target and ONE distance for the whole slide, computed over every form
   of every residue in it. Fitting each residue individually would zoom and
   pan as you clicked between them, and the entire point of posing them in a
   shared backbone frame is that nothing moves except the side chain.
   Tryptophan therefore sets the framing and glycine sits in the middle of it,
   which is itself worth seeing."
  [residues]
  (let [pts (vec (mapcat (fn [r]
                           (mapcat (fn [st]
                                     (concat (map #(get (:atoms st) %) (keys (:atoms st)))
                                             (map :base-o (:waters st))))
                                   (:states r)))
                         residues))
        c (v/centroid pts)
        far (reduce max 0 (map #(v/dist c %) pts))]
    ;; Angstroms, and nothing but angstroms -- turning an extent into a camera
    ;; distance needs a field of view, and a field of view is the renderer's
    ;; business, not this namespace's.
    {:center c :extent far}))

(defn form-index
  "Which rung of a residue's ladder is the majority species at this pH. A
   residue with one form always answers 0 -- the drawing does not move, and
   the slide says so."
  [r ph]
  (sol/majority-step ph (:pkas r)))

(defn fraction-charged
  "How much of this residue is actually carrying its side-chain charge at a
   given pH. Henderson-Hasselbalch, run in the direction the group works in:
   an acid is charged once it has LOST its proton, a base once it has kept it."
  [r ph]
  (when (:pka r)
    (let [f (sol/fraction-deprotonated ph (:pka r))]
      (if (:acid? r) f (- 1 f)))))

(def CELL-PH 7.4)
