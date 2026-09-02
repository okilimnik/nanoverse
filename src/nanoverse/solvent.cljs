(ns nanoverse.solvent
  (:require [nanoverse.vec3 :as v]))

;; The water model, shared by every "functional group in water" slide. Pure
;; chemistry and geometry -- no rendering-engine dependency at all.
;;
;; The point of this namespace is that a slide should not have to hand-place
;; its waters. Hand-placing means the picture shows what the author expected;
;; here the waters are put where the *molecule's own coordinates* say they
;; should go -- out along a computed lone pair, or out along a real X-H bond.
;; Change the molecule and the hydration shell follows, without anyone
;; retuning a magic number.

;; ---------------------------------------------------------------------
;; Water, and the geometric hydrogen-bond test
;; ---------------------------------------------------------------------

(def OH-LEN 0.9572)                              ; gas-phase water O-H
(def HOH-ANGLE (/ (* 104.5 js/Math.PI) 180))     ; gas-phase H-O-H

(def DIST-LIMIT 2.5) ; angstrom, donor-H...acceptor
(def COS-LIMIT -0.5) ; cos(120 deg): require a roughly linear D-H...A

(defn deg->rad [d] (/ (* d js/Math.PI) 180))

;; Heavy-atom...O separations for a placed water. The polar values are
;; ordinary hydrogen-bond distances -- sulfur's is much longer than oxygen's
;; because sulfur is a much bigger atom, and that difference is a real,
;; measured one rather than a nudge to make a picture work. The hydrophobic
;; distance is a van der Waals contact: close enough to look crowded, far
;; enough never to bond. Pulling it in much below this starts losing waters to
;; the clash filter on a crowded molecule like alanine, and the crowd is the
;; whole point of drawing them.
(def OO-POLAR {:O 2.85 :N 2.90 :S 3.40})
(def OO-PHOBIC 3.75)

(defn polar-oo [atoms k]
  (get OO-POLAR (:e (get atoms k)) 2.85))

;; Standard X-H bond lengths, for hydrogens we have to construct rather than
;; read out of a deposited structure.
(def XH-LEN {:N 1.010 :O 0.970 :S 1.340 :C 1.090})

;; van der Waals radii (angstrom), used only to keep auto-placed waters from
;; being dropped inside the molecule.
(def VDW {:H 1.20 :C 1.70 :N 1.55 :O 1.52 :P 1.80 :S 1.80})

;; ---------------------------------------------------------------------
;; Seeded noise
;; ---------------------------------------------------------------------

(defn mulberry32
  "Small seeded PRNG. Seeded so the wobble is identical across reloads but
   decorrelated per water / axis / term."
  [seed0]
  (let [seed (atom (bit-or seed0 0))]
    (fn []
      (swap! seed (fn [s] (bit-or (+ s 0x6D2B79F5) 0)))
      (let [s @seed
            t1 (js/Math.imul (bit-xor s (unsigned-bit-shift-right s 15)) (bit-or 1 s))
            t2 (bit-xor (+ t1 (js/Math.imul (bit-xor t1 (unsigned-bit-shift-right t1 7)) (bit-or 61 t1))) t1)]
        (/ (unsigned-bit-shift-right (bit-xor t2 (unsigned-bit-shift-right t2 14)) 0) 4294967296)))))

;; ---------------------------------------------------------------------
;; Reading a molecule
;;
;; An `atoms` map is {:O1 {:e :O :x _ :y _ :z _} ...}; `bonds` is a vector of
;; {:a :b :order}. Both come straight out of a PDB Chemical Component
;; Dictionary entry, so the element is carried as data rather than guessed
;; from the atom name.
;; ---------------------------------------------------------------------

(defn neighbours
  "Atom keys bonded to k."
  [bonds k]
  (vec (concat (map :b (filter #(= (:a %) k) bonds))
               (map :a (filter #(= (:b %) k) bonds)))))

(defn heavy? [atoms k] (not= (:e (get atoms k)) :H))

(defn recenter
  "Shift every atom so `origin` sits at (0,0,0). Passing the *same* origin to
   every protonation state of a molecule is what stops it jumping around when
   the pH slider crosses a pKa."
  [atoms origin]
  (into {} (for [k (keys atoms)] [k (merge (get atoms k) (v/sub (get atoms k) origin))])))

(defn centroid-of
  "Centroid of the named atoms (usually the heavy ones -- hydrogens come and
   go with pH and would drag the centre around)."
  [atoms ks]
  (v/centroid (map #(get atoms %) ks)))

(defn heavy-keys [atoms]
  (vec (filter #(heavy? atoms %) (keys atoms))))

;; ---------------------------------------------------------------------
;; Rigid alignment
;;
;; Two protonation states that come from two *different* CCD entries (acetic
;; acid and acetate, mercaptoethanol and its disulfide) arrive in unrelated
;; coordinate frames. Without this they would flip and spin when the slider
;; crosses the pKa, which reads as chemistry when it is only bookkeeping.
;; ---------------------------------------------------------------------

(defn- frame-of [o a b]
  (let [e1 (v/norm (v/sub a o))
        e3 (v/norm (v/cross e1 (v/sub b o)))
        e2 (v/cross e3 e1)]
    {:o o :e1 e1 :e2 e2 :e3 e3}))

(defn align
  "Rigidly move `atoms` so three of its atoms land on the frame the
   corresponding three atoms define in `ref-atoms`. `pairs` is
   [[src-key ref-key] x3].

   This is an exact three-point frame match, not a least-squares fit over all
   atoms -- the two structures are not the same molecule, so there is no
   perfect superposition to find. It only has to be rigid and repeatable."
  [atoms ref-atoms pairs]
  (let [[[p1 p2] [a1 a2] [b1 b2]] pairs
        src (frame-of (get atoms p1) (get atoms a1) (get atoms b1))
        dst (frame-of (get ref-atoms p2) (get ref-atoms a2) (get ref-atoms b2))]
    (into {}
          (for [k (keys atoms)]
            (let [d (v/sub (get atoms k) (:o src))
                  local [(v/dot d (:e1 src)) (v/dot d (:e2 src)) (v/dot d (:e3 src))]]
              [k (merge (get atoms k)
                        (v/add (:o dst)
                               (v/add (v/scale (:e1 dst) (nth local 0))
                                      (v/add (v/scale (:e2 dst) (nth local 1))
                                             (v/scale (:e3 dst) (nth local 2))))))])))))

;; ---------------------------------------------------------------------
;; Protonation
;; ---------------------------------------------------------------------

(defn deprotonate
  "Remove a hydrogen and its bond. Exact -- nothing is invented, an atom is
   simply dropped."
  [state h]
  (-> state
      (assoc :atoms (dissoc (:atoms state) h))
      (assoc :bonds (vec (remove #(or (= (:a %) h) (= (:b %) h)) (:bonds state))))))

(defn protonate
  "Add a hydrogen to `heavy`, in the one direction left over once its existing
   bonds are accounted for: opposite the sum of the unit vectors to its
   current neighbours. For an -NH2 (N bonded to C and two H) that is exactly
   the fourth tetrahedral vertex, which is where the proton goes.

   CONSTRUCTED, not measured -- the rest of the molecule is not re-relaxed
   afterwards, though a real protonation does shift the neighbouring bonds a
   little."
  [state heavy h-key]
  (let [{:keys [atoms bonds]} state
        p (get atoms heavy)
        dirs (map #(v/norm (v/sub (get atoms %) p)) (neighbours bonds heavy))
        away (v/norm (v/scale (reduce v/add {:x 0 :y 0 :z 0} dirs) -1))
        len (get XH-LEN (:e p) 1.0)]
    (-> state
        (assoc :atoms (assoc atoms h-key (merge {:e :H} (v/add p (v/scale away len)))))
        (assoc :bonds (conj bonds {:a heavy :b h-key :order 1})))))

;; ---------------------------------------------------------------------
;; Lone pairs
;;
;; Where a water has to sit if it wants to donate to us. Computed from the
;; molecule's own coordinates and the hybridisation implied by how many
;; neighbours the acceptor has -- the same construction the aldehyde slide
;; used by hand for a single carbonyl, generalised.
;; ---------------------------------------------------------------------

(def ^:private TETRAHEDRAL-HALF 54.75) ; half of 109.5

(defn- rot [axis deg vec] (v/rotate (v/quat-axis-angle axis (deg->rad deg)) vec))

(defn- any-perp [u]
  (let [ref (if (> (js/Math.abs (:y u)) 0.9) {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0})]
    (v/norm (v/cross u ref))))

(defn lone-pair-dirs
  "Unit vectors pointing out of atom `k` along its lone pairs.

   - two neighbours (an -OH oxygen, an -SH sulfur, an ester oxygen): sp3, two
     pairs at +/- 54.75 deg off the reverse bisector
   - three neighbours (an amine nitrogen): sp3, the single remaining vertex
   - one neighbour that is a carbon (a carbonyl oxygen): sp2, two pairs at
     +/- 60 deg in the plane the carbon's own substituents define
   - one neighbour that is not a carbon (a phosphate oxygen): the pairs fan
     out on a cone rather than lying in a plane, so three are offered"
  [atoms bonds k]
  (let [p (get atoms k)
        nbrs (neighbours bonds k)
        us (mapv #(v/norm (v/sub (get atoms %) p)) nbrs)
        n (count us)]
    (cond
      (= n 3)
      [(v/norm (v/scale (reduce v/add {:x 0 :y 0 :z 0} us) -1))]

      (= n 2)
      (let [away (v/norm (v/scale (v/add (nth us 0) (nth us 1)) -1))
            axis (v/norm (v/cross (nth us 0) (nth us 1)))]
        [(rot axis TETRAHEDRAL-HALF away) (rot axis (- TETRAHEDRAL-HALF) away)])

      (= n 1)
      (let [nb (nth nbrs 0)
            axis (v/scale (nth us 0) -1)                    ; neighbour -> k, extended
            others (vec (remove #(= % k) (neighbours bonds nb)))
            heavy-other (first (filter #(heavy? atoms %) others))]
        (if (and (= (:e (get atoms nb)) :C) heavy-other)
          ;; sp2: both pairs lie in the plane through k, its neighbour, and
          ;; that neighbour's other substituent
          (let [normal (v/norm (v/cross (v/sub (get atoms heavy-other) (get atoms nb))
                                        (v/sub p (get atoms nb))))]
            [(rot normal 60 axis) (rot normal -60 axis)])
          ;; a terminal oxygen on a tetrahedral centre (P, S): the pairs sit
          ;; on a cone about the bond, so sample it at three azimuths
          (let [e2 (any-perp axis)
                tilt (v/add (v/scale axis (js/Math.cos (deg->rad 70.5)))
                            (v/scale e2 (js/Math.sin (deg->rad 70.5))))]
            [tilt (rot axis 120 tilt) (rot axis 240 tilt)])))

      :else [])))

;; ---------------------------------------------------------------------
;; Placing the waters
;; ---------------------------------------------------------------------

(defn build-basis
  "Orthonormal pair for a water's own frame. :donor aims e1 back at the
   molecule, so the water leads with one of its hydrogens; :acceptor aims e1
   away, so it leads with its oxygen and its hydrogens point off into bulk."
  [away role]
  (let [e1 (if (= role :donor) (v/scale away -1) away)
        ref (if (> (js/Math.abs (:y e1)) 0.9) {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0})]
    {:e1 e1 :e2 (v/norm (v/cross e1 ref))}))

(defn water-atoms [basis]
  {:O {:x 0 :y 0 :z 0}
   :H1 (v/scale (:e1 basis) OH-LEN)
   :H2 (v/add (v/scale (:e1 basis) (* (js/Math.cos HOH-ANGLE) OH-LEN))
              (v/scale (:e2 basis) (* (js/Math.sin HOH-ANGLE) OH-LEN)))})

(defn- make-water
  "One water: its frame, its per-axis wobble, and its tumble.

   Every draw is bound in an explicit `let`, in order, rather than written
   inline in the returned map. That is not style. `repeatedly` is LAZY in
   Squint, so a wobble built inline as a map value can be realized *after* the
   tumble draws that follow it in the literal -- which means the numbers a
   water gets depend on when something first reads the field. Seeded noise is
   only reproducible if the draw order is fixed, so it is fixed here: 12 draws
   of wobble, then 3 of tumble axis, then freq, then phase. 17 per water."
  [rng nm site role away base-o]
  (let [basis (build-basis away role)
        wobble (mapv (fn [_]
                       [{:freq (+ 0.6 (* (rng) 0.5)) :phase (* (rng) js/Math.PI 2) :amp 0.62}
                        {:freq (+ 1.3 (* (rng) 0.9)) :phase (* (rng) js/Math.PI 2) :amp 0.38}])
                     [0 1 2])
        tumble-axis (v/norm {:x (- (rng) 0.5) :y (- (rng) 0.5) :z (- (rng) 0.5)})
        tumble-freq (+ 0.5 (* (rng) 0.7))
        tumble-phase (* (rng) js/Math.PI 2)]
    {:name nm :site site :role role :basis basis :local (water-atoms basis) :base-o base-o
     :wobble wobble :tumble-axis tumble-axis
     :tumble-freq tumble-freq :tumble-phase tumble-phase}))

(defn- clashes?
  "Reject a candidate water that would land inside the molecule or on top of a
   water already placed. `skip` is the atom it is being placed against, which
   it is of course allowed to be close to."
  [atoms placed base-o skip]
  (or (some (fn [k]
              (and (not= k skip)
                   (< (v/len (v/sub base-o (get atoms k)))
                      (+ (get VDW (:e (get atoms k)) 1.6) 0.9))))
            (heavy-keys atoms))
      ;; two waters closer than a water-water hydrogen bond are on top of
      ;; each other, not next to each other
      (some (fn [w] (< (v/len (v/sub base-o (:base-o w))) 2.70)) placed)))

(defn place
  "Build waters from explicit definitions instead of deriving them from the
   structure. Each def is {:name :site :role :anchor :away :oo}, and the water
   lands at (anchor + away * oo).

   `hydrate` is the default and the better habit: it puts waters where the
   molecule's own coordinates say they go, so nobody can quietly arrange the
   answer. This exists for the case where a slide is deliberately calibrating
   its shell AND says so on its face -- the hydroxyl slide tunes its resting
   O...O distances to sit just inside the bond cutoff, so the default jitter
   carries them back out and the count visibly breathes. That flicker is the
   whole lesson of that slide, and it is listed in its own LIMITS as tuned
   rather than measured.

   Same water shape and the same 17-draw RNG stride as `hydrate`, so the two
   are interchangeable everywhere downstream."
  [state defs {:keys [seed] :or {seed 20260902}}]
  (let [rng (mulberry32 seed)
        atoms (:atoms state)]
    (assoc state :waters
           (mapv (fn [d]
                   (make-water rng (:name d) (:site d) (:role d) (:away d)
                               (v/add (get atoms (:anchor d))
                                      (v/scale (:away d) (:oo d)))))
                 defs))))

(defn hydrate
  "Put a hydration shell around a molecule, derived from its own geometry.

   For each acceptor, a water is placed out along each computed lone pair,
   turned so one of its hydrogens points back in. For each polar hydrogen, a
   water is placed out along that real X-H bond with its oxygen facing in. For
   each hydrophobic site, waters are placed at van der Waals contact -- they
   are there precisely because nothing will ever happen to them.

   Candidates that would overlap the molecule or an already-placed water are
   dropped, so a crowded group like a phosphate ends up with a plausible shell
   instead of a pile."
  [state {:keys [seed hydrophobic phobic-count]
          :or {seed 20260902 phobic-count 2}}]
  (let [{:keys [atoms bonds acceptors donors]} state
        rng (mulberry32 seed)]
    (letfn [(add [placed nm site role away anchor oo skip]
              (let [base-o (v/add (get atoms anchor) (v/scale away oo))]
                (if (clashes? atoms placed base-o skip)
                  placed
                  (conj placed (make-water rng nm site role away base-o)))))]
      (let [;; Waters that ACCEPT one of our polar hydrogens, straight out along
            ;; the real X-H bond. These go FIRST, and the order matters: a
            ;; donor's water has exactly one place it can be, fixed by a
            ;; measured bond direction, whereas a lone pair offers two or three
            ;; alternatives. Placing the constrained ones first and letting the
            ;; flexible ones fill in around them is what stops a molecule's one
            ;; acidic proton from silently losing its water to a lone pair that
            ;; had somewhere else to go.
            after-don
            (reduce (fn [placed d]
                      (let [dir (v/norm (v/sub (get atoms (:h d)) (get atoms (:heavy d))))]
                        (add placed (str (name (:h d)) "-acc") (:site d) :acceptor
                             dir (:heavy d) (or (:oo d) (polar-oo atoms (:heavy d))) (:heavy d))))
                    []
                    (or donors []))
            ;; waters that donate INTO one of our lone pairs
            after-acc
            (reduce (fn [placed a]
                      (reduce (fn [pl [i d]]
                                (add pl (str (name (:atom a)) "-lp" i) (:site a) :donor
                                     d (:atom a) (or (:oo a) (polar-oo atoms (:atom a))) (:atom a)))
                              placed
                              (map-indexed vector
                                           (take (or (:max-lp a) 2)
                                                 (lone-pair-dirs atoms bonds (:atom a))))))
                    after-don
                    (or acceptors []))
            ;; waters that just sit against a greasy surface
            after-phobic
            (reduce (fn [placed hsite]
                      (let [k (:atom hsite)
                            hs (filter #(= (:e (get atoms %)) :H) (neighbours bonds k))
                            out (if (seq hs)
                                  ;; a -CH3 or -CH2 points away from the chain
                                  ;; along the average of its own C-H bonds
                                  (v/norm (v/centroid (map #(v/sub (get atoms %) (get atoms k)) hs)))
                                  (v/norm (v/sub (get atoms k) (centroid-of atoms (heavy-keys atoms)))))
                            perp (any-perp out)]
                        (reduce (fn [pl i]
                                  (let [tilt (v/add (v/scale out (js/Math.cos (deg->rad 32)))
                                                    (v/scale perp (js/Math.sin (deg->rad 32))))
                                        d (rot out (* i (/ 360 (or (:n hsite) phobic-count))) tilt)]
                                    (add pl (str (name k) "-ph" i) (:site hsite) :donor
                                         d k OO-PHOBIC k)))
                                placed
                                (range (or (:n hsite) phobic-count)))))
                    after-acc
                    (or hydrophobic []))]
        (assoc state :waters (vec after-phobic))))))

;; ---------------------------------------------------------------------
;; Motion
;; ---------------------------------------------------------------------

(defn jitter-for [water t]
  (into {}
        (map-indexed
          (fn [i ax]
            [ax (reduce + (map (fn [term] (* (:amp term) (js/Math.sin (+ (* t (:freq term)) (:phase term)))))
                               (nth (:wobble water) i)))])
          [:x :y :z])))

(defn compute-current
  "World-space state of one water at time t. `amp` is translational jitter in
   angstroms, `tumble-deg` the peak rotation of the whole water about its own
   axis. The quaternion is returned too, so the renderer orients the drawn
   water with the exact same rotation the hydrogens here were given."
  [w t amp tumble-deg]
  (let [o (v/add (:base-o w) (v/scale (jitter-for w t) amp))
        theta (* (deg->rad tumble-deg)
                 (js/Math.sin (+ (* t (:tumble-freq w)) (:tumble-phase w))))
        q (v/quat-axis-angle (:tumble-axis w) theta)
        local (:local w)]
    {:O o
     :H1 (v/add o (v/rotate q (:H1 local)))
     :H2 (v/add o (v/rotate q (:H2 local)))
     :quat q}))

;; ---------------------------------------------------------------------
;; Hydrogen-bond candidates
;;
;; Every water is checked against every acceptor and every polar hydrogen the
;; molecule declares. A group with no polar hydrogen contributes no
;; :molecule-donates pairs at all, so a "gives 0" readout is zero by
;; construction rather than by a threshold that happens never to be met.
;; ---------------------------------------------------------------------

(defn candidate-pairs [state]
  (let [n (count (:waters state))]
    (vec (concat
           (for [i (range n)
                 a (or (:acceptors state) [])
                 h [:H1 :H2]]
             {:kind :water-donates :water i :water-h h
              :acceptor (:atom a) :site (:site a) :pair-e [:O (:e (get (:atoms state) (:atom a)))]})
           (for [i (range n)
                 d (or (:donors state) [])]
             {:kind :molecule-donates :water i :donor-h (:h d) :donor-heavy (:heavy d)
              :site (:site d) :pair-e [(:e (get (:atoms state) (:heavy d))) :O]})))))

;; Typical hydrogen-bond enthalpies (kJ/mol) by donor->acceptor element pair.
;; TABULATED literature ballparks, NOT computed from the geometry on screen --
;; they exist so a slide can say out loud that a distance-and-angle test
;; cannot tell a strong bond from a weak one.
(def STRENGTH {"O>O" 21 "O>N" 20 "N>O" 17 "N>N" 13 "S>O" 7 "O>S" 6 "S>S" 4})

(defn strength-of [pair]
  (let [[d a] (:pair-e pair)]
    (get STRENGTH (str (name d) ">" (name a)) 15)))

(defn evaluate
  "Geometry of one candidate pair against the current waters. Reports why a
   near contact is not a bond, which is the part a distance-only picture
   throws away."
  [pair atoms current]
  (let [c (nth current (:water pair))
        [D H A] (if (= (:kind pair) :water-donates)
                  [(:O c) (get c (:water-h pair)) (get atoms (:acceptor pair))]
                  [(get atoms (:donor-heavy pair)) (get atoms (:donor-h pair)) (:O c)])
        d-vec (v/sub A H)
        dist (v/len d-vec)
        cos-a (v/dot (v/norm (v/sub D H)) (v/norm d-vec))
        near? (< dist DIST-LIMIT)
        linear? (< cos-a COS-LIMIT)]
    {:H H :A A :dist dist
     :angle (/ (* (js/Math.acos (js/Math.max -1 (js/Math.min 1 cos-a))) 180) js/Math.PI)
     :active (and near? linear?)
     :bent (and near? (not linear?))
     :kind (:kind pair)
     :site (:site pair)
     :strength (strength-of pair)}))

;; ---------------------------------------------------------------------
;; Titration
;; ---------------------------------------------------------------------

(defn fraction-deprotonated
  "Henderson-Hasselbalch: the fraction of molecules that have already given
   this proton up at a given pH."
  [ph pka]
  (/ 1 (+ 1 (js/Math.pow 10 (- pka ph)))))

(defn signed-charge
  "Format a formal charge the way chemistry writes it."
  [q]
  (cond (zero? q) "0" (pos? q) (str "+" q) :else (str q)))

(defn majority-step
  "How many protons of an ordered (ascending pKa) ladder are off, for the
   single species we draw. The picture can only show one molecule, so it shows
   the majority species; `fraction-deprotonated` is what says how rough that
   is right at a pKa."
  [ph pkas]
  (count (filter #(> ph %) pkas)))
