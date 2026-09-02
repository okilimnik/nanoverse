(ns nanoverse.aldehyde.geometry
  (:require [nanoverse.vec3 :as v]
            [nanoverse.solvent :as sol]))

;; Pure chemistry/physics for the aldehyde scene -- no rendering-engine
;; dependency. The water model, the tumble and the hydrogen-bond test are
;; shared, in nanoverse.solvent; this file is acetaldehyde and its shell.
;;
;; ---------------------------------------------------------------------
;; Real geometry, in angstroms. Acetaldehyde (CH3-CHO), PubChem CID 177
;; 3D conformer, kept verbatim in structures/acetaldehyde.sdf.
;;
;; Provenance note: acetaldehyde is too small and volatile to appear as a
;; PDB ligand, so unlike every other molecule in this deck it is a *computed*
;; conformer rather than a deposited component. It reproduces the microwave
;; structure closely -- C=O 1.226 vs 1.216 A, C-C 1.498 vs 1.501 A,
;; C-C=O 123.4 vs 124.1 deg -- and its CHO group is planar to 0.0007 A,
;; which is what the sp2 lone-pair construction relies on.
;;
;; Acetaldehyde is what the ethanol on the previous slide becomes: alcohol
;; dehydrogenase oxidises the -OH to a -CHO. Same two carbons, one group
;; swapped, and the hydrogen-bonding behaviour changes completely.
;; ---------------------------------------------------------------------

(def mol-raw
  {:C1  {:e :C :x  0.1130 :y -0.4226 :z  0.0000}   ; carbonyl (aldehyde) carbon
   :O1  {:e :O :x  1.1443 :y  0.2412 :z  0.0000}   ; carbonyl oxygen -- ACCEPTOR ONLY
   :H1  {:e :H :x  0.1478 :y -1.5252 :z -0.0007}   ; aldehyde C-H (not an H-bond donor)
   :C2  {:e :C :x -1.2574 :y  0.1815 :z  0.0000}   ; methyl carbon
   :H2a {:e :H :x -1.7938 :y -0.1493 :z  0.8924}
   :H2b {:e :H :x -1.1865 :y  1.2719 :z  0.0016}
   :H2c {:e :H :x -1.7928 :y -0.1468 :z -0.8938}})

(def mol-keys (keys mol-raw))

(def mol (sol/recenter mol-raw (sol/centroid-of mol-raw mol-keys)))

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
;;
;; This construction is now the shared solvent/lone-pair-dirs, which every
;; later slide uses too -- it was generalised out of exactly this code.
;; ---------------------------------------------------------------------

(def carbonyl-normal
  (v/norm (v/cross (v/sub (:C2 mol) (:C1 mol))
                   (v/sub (:O1 mol) (:C1 mol)))))

(def lone-pairs (sol/lone-pair-dirs mol mol-bonds :O1))
(def lone-pair-a (nth lone-pairs 0))
(def lone-pair-b (nth lone-pairs 1))

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
;; sit at the edge of the cutoff the way the previous slide's are. Bonds break
;; here because the tumble swings the hydrogen off-axis, not because the
;; resting geometry was rigged.
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

;; ---------------------------------------------------------------------
;; What the group can do.
;;
;; The asymmetry is the lesson, and it is expressed as data rather than as
;; prose: `:donors` is EMPTY. Acetaldehyde has no polar hydrogen anywhere on
;; it -- the only H on the carbonyl carbon is a C-H, and carbon is not
;; electronegative enough to make it a donor. So solvent/candidate-pairs
;; cannot produce a single pair with this molecule on the donating side, and
;; the "gives" readout is pinned at zero by construction, not by a threshold
;; that happens never to be met.
;;
;; Contrast the hydroxyl slide, where the same list has one entry.
;; ---------------------------------------------------------------------

(def state
  (sol/place
    {:label "acetaldehyde"
     :atoms mol
     :bonds mol-bonds
     :charge 0
     :acceptors [{:atom :O1 :site :carbonyl}]
     :donors []}
    water-defs
    {:seed 20260831}))
