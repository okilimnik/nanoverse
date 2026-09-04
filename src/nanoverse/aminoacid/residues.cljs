(ns nanoverse.aminoacid.residues)

;; GENERATED FILE -- do not edit by hand.
;;
;;   node tools/cif_to_cljs.mjs > src/nanoverse/aminoacid/residues.cljs
;;
;; The twenty standard amino acids plus selenocysteine, straight out of the PDB
;; Chemical Component Dictionary: ideal coordinates in angstroms, elements, and
;; bond orders, all read off the .cif files kept verbatim in structures/.
;;
;; Read this as data, not as code. What each side chain is ALLOWED TO DO --
;; which atom accepts, which hydrogen donates, which surface is greasy, what
;; the pKa is -- is declared next door in nanoverse.aminoacid.geometry, by
;; hand, because those are chemical claims and they should be somewhere a
;; person can check them.
;;
;; Two things here are already an interpretation and are worth knowing about:
;;
;;   * An aromatic bond is emitted as :order 1.5. The file writes each ring
;;     bond as SING or DOUB because a CIF has to commit to one Kekule
;;     structure; the ring's electrons are not actually arranged that way, and
;;     the deck already draws a delocalised bond as one-and-a-half.
;;
;;   * SOME OF THESE ENTRIES ARE ALREADY IONS. The CCD deposits arginine,
;;     lysine and histidine in their protonated, cationic forms (formal charge
;;     +1) and aspartate/glutamate in their neutral acid forms. That is noted
;;     per residue below, and it decides which direction each slide has to
;;     construct from -- removing an atom is exact, adding one is not.

;; ---------------------------------------------------------------------
;; ASP -- aspartic acid, C4 H7 N O4, formal charge 0
;; ---------------------------------------------------------------------
(def ASP
  {:code "ASP"
   :formula "C4 H7 N O4"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x   -0.317 :y    1.688 :z    0.066}
    :CA  {:e :C :x   -0.470 :y    0.286 :z   -0.344}
    :C   {:e :C :x   -1.868 :y   -0.180 :z   -0.029}
    :O   {:e :O :x   -2.534 :y    0.415 :z    0.786}
    :CB  {:e :C :x    0.539 :y   -0.580 :z    0.413}
    :CG  {:e :C :x    1.938 :y   -0.195 :z    0.004}
    :OD1 {:e :O :x    2.109 :y    0.681 :z   -0.810}
    :OD2 {:e :O :x    2.992 :y   -0.826 :z    0.543}
    :OXT {:e :O :x   -2.374 :y   -1.256 :z   -0.652}
    :H   {:e :H :x   -0.928 :y    2.289 :z   -0.467}
    :H2  {:e :H :x   -0.478 :y    1.795 :z    1.056}
    :HA  {:e :H :x   -0.292 :y    0.199 :z   -1.416}
    :HB2 {:e :H :x    0.419 :y   -0.425 :z    1.485}
    :HB3 {:e :H :x    0.367 :y   -1.630 :z    0.176}
    :HD2 {:e :H :x    3.869 :y   -0.545 :z    0.250}
    :HXT {:e :H :x   -3.275 :y   -1.517 :z   -0.416}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :OD1 :order 2  }
    {:a :CG  :b :OD2 :order 1  }
    {:a :OD2 :b :HD2 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; GLU -- glutamic acid, C5 H9 N O4, formal charge 0
;; ---------------------------------------------------------------------
(def GLU
  {:code "GLU"
   :formula "C5 H9 N O4"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.199 :y    1.867 :z   -0.117}
    :CA  {:e :C :x    1.138 :y    0.515 :z    0.453}
    :C   {:e :C :x    2.364 :y   -0.260 :z    0.041}
    :O   {:e :O :x    3.010 :y    0.096 :z   -0.916}
    :CB  {:e :C :x   -0.113 :y   -0.200 :z   -0.062}
    :CG  {:e :C :x   -1.360 :y    0.517 :z    0.461}
    :CD  {:e :C :x   -2.593 :y   -0.187 :z   -0.046}
    :OE1 {:e :O :x   -2.485 :y   -1.161 :z   -0.753}
    :OE2 {:e :O :x   -3.811 :y    0.269 :z    0.287}
    :OXT {:e :O :x    2.737 :y   -1.345 :z    0.737}
    :H   {:e :H :x    1.237 :y    1.834 :z   -1.125}
    :H2  {:e :H :x    0.421 :y    2.427 :z    0.197}
    :HA  {:e :H :x    1.098 :y    0.580 :z    1.540}
    :HB2 {:e :H :x   -0.117 :y   -0.187 :z   -1.152}
    :HB3 {:e :H :x   -0.113 :y   -1.231 :z    0.289}
    :HG2 {:e :H :x   -1.357 :y    0.504 :z    1.551}
    :HG3 {:e :H :x   -1.360 :y    1.548 :z    0.109}
    :HE2 {:e :H :x   -4.571 :y   -0.215 :z   -0.062}
    :HXT {:e :H :x    3.530 :y   -1.809 :z    0.435}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD  :order 1  }
    {:a :CG  :b :HG2 :order 1  }
    {:a :CG  :b :HG3 :order 1  }
    {:a :CD  :b :OE1 :order 2  }
    {:a :CD  :b :OE2 :order 1  }
    {:a :OE2 :b :HE2 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; LYS -- lysine, C6 H15 N2 O2, formal charge 1
;; ---------------------------------------------------------------------
(def LYS
  {:code "LYS"
   :formula "C6 H15 N2 O2"
   :formal-charge 1
   :atoms
   {:N   {:e :N :x    1.422 :y    1.796 :z    0.198}
    :CA  {:e :C :x    1.394 :y    0.355 :z    0.484}
    :C   {:e :C :x    2.657 :y   -0.284 :z   -0.032}
    :O   {:e :O :x    3.316 :y    0.275 :z   -0.876}
    :CB  {:e :C :x    0.184 :y   -0.278 :z   -0.206}
    :CG  {:e :C :x   -1.102 :y    0.282 :z    0.407}
    :CD  {:e :C :x   -2.313 :y   -0.351 :z   -0.283}
    :CE  {:e :C :x   -3.598 :y    0.208 :z    0.329}
    :NZ  {:e :N :x   -4.761 :y   -0.400 :z   -0.332}  ; formal charge +1
    :OXT {:e :O :x    3.050 :y   -1.476 :z    0.446}
    :H   {:e :H :x    1.489 :y    1.891 :z   -0.804}
    :H2  {:e :H :x    0.521 :y    2.162 :z    0.464}
    :HA  {:e :H :x    1.322 :y    0.200 :z    1.560}
    :HB2 {:e :H :x    0.210 :y   -0.047 :z   -1.270}
    :HB3 {:e :H :x    0.211 :y   -1.359 :z   -0.068}
    :HG2 {:e :H :x   -1.128 :y    0.050 :z    1.471}
    :HG3 {:e :H :x   -1.130 :y    1.363 :z    0.269}
    :HD2 {:e :H :x   -2.287 :y   -0.120 :z   -1.348}
    :HD3 {:e :H :x   -2.285 :y   -1.432 :z   -0.145}
    :HE2 {:e :H :x   -3.625 :y   -0.023 :z    1.394}
    :HE3 {:e :H :x   -3.626 :y    1.289 :z    0.192}
    :HZ1 {:e :H :x   -4.736 :y   -0.185 :z   -1.318}
    :HZ2 {:e :H :x   -4.735 :y   -1.400 :z   -0.205}
    :HZ3 {:e :H :x   -5.609 :y   -0.031 :z    0.071}
    :HXT {:e :H :x    3.861 :y   -1.886 :z    0.115}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD  :order 1  }
    {:a :CG  :b :HG2 :order 1  }
    {:a :CG  :b :HG3 :order 1  }
    {:a :CD  :b :CE  :order 1  }
    {:a :CD  :b :HD2 :order 1  }
    {:a :CD  :b :HD3 :order 1  }
    {:a :CE  :b :NZ  :order 1  }
    {:a :CE  :b :HE2 :order 1  }
    {:a :CE  :b :HE3 :order 1  }
    {:a :NZ  :b :HZ1 :order 1  }
    {:a :NZ  :b :HZ2 :order 1  }
    {:a :NZ  :b :HZ3 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; ARG -- arginine, C6 H15 N4 O2, formal charge 1
;; ---------------------------------------------------------------------
(def ARG
  {:code "ARG"
   :formula "C6 H15 N4 O2"
   :formal-charge 1
   :atoms
   {:N    {:e :N :x   -0.469 :y    1.110 :z   -0.993}
    :CA   {:e :C :x    0.004 :y    2.294 :z   -1.708}
    :C    {:e :C :x   -0.907 :y    2.521 :z   -2.901}
    :O    {:e :O :x   -1.827 :y    1.789 :z   -3.242}
    :CB   {:e :C :x    1.475 :y    2.150 :z   -2.127}
    :CG   {:e :C :x    1.745 :y    1.017 :z   -3.130}
    :CD   {:e :C :x    3.210 :y    0.954 :z   -3.557}
    :NE   {:e :N :x    4.071 :y    0.726 :z   -2.421}
    :CZ   {:e :C :x    5.469 :y    0.624 :z   -2.528}
    :NH1  {:e :N :x    6.259 :y    0.404 :z   -1.405}
    :NH2  {:e :N :x    6.078 :y    0.744 :z   -3.773}  ; formal charge +1
    :OXT  {:e :O :x   -0.588 :y    3.659 :z   -3.574}
    :H    {:e :H :x   -0.058 :y    0.903 :z   -0.109}
    :H2   {:e :H :x   -1.024 :y    0.452 :z   -1.494}
    :HA   {:e :H :x   -0.103 :y    3.152 :z   -1.034}
    :HB2  {:e :H :x    2.086 :y    1.988 :z   -1.230}
    :HB3  {:e :H :x    1.814 :y    3.099 :z   -2.563}
    :HG2  {:e :H :x    1.136 :y    1.170 :z   -4.029}
    :HG3  {:e :H :x    1.447 :y    0.054 :z   -2.698}
    :HD2  {:e :H :x    3.348 :y    0.133 :z   -4.269}
    :HD3  {:e :H :x    3.505 :y    1.880 :z   -4.062}
    :HE   {:e :H :x    3.674 :y    0.627 :z   -1.479}
    :HH11 {:e :H :x    7.271 :y    0.331 :z   -1.484}
    :HH12 {:e :H :x    5.858 :y    0.307 :z   -0.476}
    :HH21 {:e :H :x    5.530 :y    0.906 :z   -4.614}
    :HH22 {:e :H :x    7.088 :y    0.675 :z   -3.874}
    :HXT  {:e :H :x   -1.149 :y    3.855 :z   -4.355}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG   :order 1  }
    {:a :CB   :b :HB2  :order 1  }
    {:a :CB   :b :HB3  :order 1  }
    {:a :CG   :b :CD   :order 1  }
    {:a :CG   :b :HG2  :order 1  }
    {:a :CG   :b :HG3  :order 1  }
    {:a :CD   :b :NE   :order 1  }
    {:a :CD   :b :HD2  :order 1  }
    {:a :CD   :b :HD3  :order 1  }
    {:a :NE   :b :CZ   :order 1  }
    {:a :NE   :b :HE   :order 1  }
    {:a :CZ   :b :NH1  :order 1  }
    {:a :CZ   :b :NH2  :order 2  }
    {:a :NH1  :b :HH11 :order 1  }
    {:a :NH1  :b :HH12 :order 1  }
    {:a :NH2  :b :HH21 :order 1  }
    {:a :NH2  :b :HH22 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; HIS -- histidine, C6 H10 N3 O2, formal charge 1
;; ---------------------------------------------------------------------
(def HIS
  {:code "HIS"
   :formula "C6 H10 N3 O2"
   :formal-charge 1
   :atoms
   {:N   {:e :N :x   -0.040 :y   -1.210 :z    0.053}
    :CA  {:e :C :x    1.172 :y   -1.709 :z    0.652}
    :C   {:e :C :x    1.083 :y   -3.207 :z    0.905}
    :O   {:e :O :x    0.040 :y   -3.770 :z    1.222}
    :CB  {:e :C :x    1.484 :y   -0.975 :z    1.962}
    :CG  {:e :C :x    2.940 :y   -1.060 :z    2.353}
    :ND1 {:e :N :x    3.380 :y   -2.075 :z    3.129}  ; formal charge +1
    :CD2 {:e :C :x    3.960 :y   -0.251 :z    2.046}
    :CE1 {:e :C :x    4.693 :y   -1.908 :z    3.317}
    :NE2 {:e :N :x    5.058 :y   -0.801 :z    2.662}
    :OXT {:e :O :x    2.247 :y   -3.882 :z    0.744}
    :H   {:e :H :x   -0.102 :y   -1.155 :z   -0.950}
    :H2  {:e :H :x   -0.715 :y   -0.741 :z    0.634}
    :HA  {:e :H :x    1.965 :y   -1.558 :z   -0.089}
    :HB2 {:e :H :x    1.215 :y    0.087 :z    1.879}
    :HB3 {:e :H :x    0.859 :y   -1.368 :z    2.775}
    :HD1 {:e :H :x    2.828 :y   -2.838 :z    3.511}
    :HD2 {:e :H :x    4.108 :y    0.647 :z    1.479}
    :HE1 {:e :H :x    5.340 :y   -2.550 :z    3.892}
    :HE2 {:e :H :x    6.002 :y   -0.428 :z    2.627}
    :HXT {:e :H :x    2.188 :y   -4.848 :z    0.901}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :ND1 :order 1.5}  ; aromatic: file says SING
    {:a :CG  :b :CD2 :order 1.5}  ; aromatic: file says DOUB
    {:a :ND1 :b :CE1 :order 1.5}  ; aromatic: file says DOUB
    {:a :ND1 :b :HD1 :order 1  }
    {:a :CD2 :b :NE2 :order 1.5}  ; aromatic: file says SING
    {:a :CD2 :b :HD2 :order 1  }
    {:a :CE1 :b :NE2 :order 1.5}  ; aromatic: file says SING
    {:a :CE1 :b :HE1 :order 1  }
    {:a :NE2 :b :HE2 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; SER -- serine, C3 H7 N O3, formal charge 0
;; ---------------------------------------------------------------------
(def SER
  {:code "SER"
   :formula "C3 H7 N O3"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.525 :y    0.493 :z   -0.608}
    :CA  {:e :C :x    0.100 :y    0.469 :z   -0.252}
    :C   {:e :C :x   -0.053 :y    0.004 :z    1.173}
    :O   {:e :O :x    0.751 :y   -0.760 :z    1.649}
    :CB  {:e :C :x   -0.642 :y   -0.489 :z   -1.184}
    :OG  {:e :O :x   -0.496 :y   -0.049 :z   -2.535}
    :OXT {:e :O :x   -1.084 :y    0.440 :z    1.913}
    :H   {:e :H :x    1.867 :y   -0.449 :z   -0.499}
    :H2  {:e :H :x    1.574 :y    0.707 :z   -1.593}
    :HA  {:e :H :x   -0.316 :y    1.471 :z   -0.354}
    :HB2 {:e :H :x   -0.225 :y   -1.491 :z   -1.081}
    :HB3 {:e :H :x   -1.699 :y   -0.507 :z   -0.920}
    :HG  {:e :H :x   -0.978 :y   -0.679 :z   -3.088}
    :HXT {:e :H :x   -1.183 :y    0.142 :z    2.828}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :OG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :OG  :b :HG  :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; THR -- threonine, C4 H9 N O3, formal charge 0
;; ---------------------------------------------------------------------
(def THR
  {:code "THR"
   :formula "C4 H9 N O3"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x    1.543 :y   -0.702 :z    0.430}
    :CA   {:e :C :x    0.122 :y   -0.706 :z    0.056}
    :C    {:e :C :x   -0.038 :y   -0.090 :z   -1.309}
    :O    {:e :O :x    0.732 :y    0.761 :z   -1.683}
    :CB   {:e :C :x   -0.675 :y    0.104 :z    1.079}
    :OG1  {:e :O :x   -0.193 :y    1.448 :z    1.103}
    :CG2  {:e :C :x   -0.511 :y   -0.521 :z    2.466}
    :OXT  {:e :O :x   -1.039 :y   -0.488 :z   -2.110}
    :H    {:e :H :x    1.839 :y    0.261 :z    0.434}
    :H2   {:e :H :x    1.593 :y   -1.025 :z    1.385}
    :HA   {:e :H :x   -0.245 :y   -1.732 :z    0.038}
    :HB   {:e :H :x   -1.729 :y    0.101 :z    0.802}
    :HG1  {:e :H :x    0.740 :y    1.406 :z    1.352}
    :HG21 {:e :H :x   -1.080 :y    0.056 :z    3.194}
    :HG22 {:e :H :x   -0.879 :y   -1.547 :z    2.448}
    :HG23 {:e :H :x    0.542 :y   -0.518 :z    2.743}
    :HXT  {:e :H :x   -1.143 :y   -0.092 :z   -2.986}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :OG1  :order 1  }
    {:a :CB   :b :CG2  :order 1  }
    {:a :CB   :b :HB   :order 1  }
    {:a :OG1  :b :HG1  :order 1  }
    {:a :CG2  :b :HG21 :order 1  }
    {:a :CG2  :b :HG22 :order 1  }
    {:a :CG2  :b :HG23 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; ASN -- asparagine, C4 H8 N2 O3, formal charge 0
;; ---------------------------------------------------------------------
(def ASN
  {:code "ASN"
   :formula "C4 H8 N2 O3"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x   -0.293 :y    1.686 :z    0.094}
    :CA   {:e :C :x   -0.448 :y    0.292 :z   -0.340}
    :C    {:e :C :x   -1.846 :y   -0.179 :z   -0.031}
    :O    {:e :O :x   -2.510 :y    0.402 :z    0.794}
    :CB   {:e :C :x    0.562 :y   -0.588 :z    0.401}
    :CG   {:e :C :x    1.960 :y   -0.197 :z   -0.002}
    :OD1  {:e :O :x    2.132 :y    0.697 :z   -0.804}
    :ND2  {:e :N :x    3.019 :y   -0.841 :z    0.527}
    :OXT  {:e :O :x   -2.353 :y   -1.243 :z   -0.673}
    :H    {:e :H :x   -0.904 :y    2.297 :z   -0.427}
    :H2   {:e :H :x   -0.453 :y    1.776 :z    1.086}
    :HA   {:e :H :x   -0.270 :y    0.223 :z   -1.413}
    :HB2  {:e :H :x    0.442 :y   -0.451 :z    1.476}
    :HB3  {:e :H :x    0.389 :y   -1.633 :z    0.146}
    :HD21 {:e :H :x    2.881 :y   -1.556 :z    1.168}
    :HD22 {:e :H :x    3.919 :y   -0.590 :z    0.268}
    :HXT  {:e :H :x   -3.254 :y   -1.508 :z   -0.441}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG   :order 1  }
    {:a :CB   :b :HB2  :order 1  }
    {:a :CB   :b :HB3  :order 1  }
    {:a :CG   :b :OD1  :order 2  }
    {:a :CG   :b :ND2  :order 1  }
    {:a :ND2  :b :HD21 :order 1  }
    {:a :ND2  :b :HD22 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; GLN -- glutamine, C5 H10 N2 O3, formal charge 0
;; ---------------------------------------------------------------------
(def GLN
  {:code "GLN"
   :formula "C5 H10 N2 O3"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x    1.858 :y   -0.148 :z    1.125}
    :CA   {:e :C :x    0.517 :y    0.451 :z    1.112}
    :C    {:e :C :x   -0.236 :y    0.022 :z    2.344}
    :O    {:e :O :x   -0.005 :y   -1.049 :z    2.851}
    :CB   {:e :C :x   -0.236 :y   -0.013 :z   -0.135}
    :CG   {:e :C :x    0.529 :y    0.421 :z   -1.385}
    :CD   {:e :C :x   -0.213 :y   -0.036 :z   -2.614}
    :OE1  {:e :O :x   -1.252 :y   -0.650 :z   -2.500}
    :NE2  {:e :N :x    0.277 :y    0.236 :z   -3.839}
    :OXT  {:e :O :x   -1.165 :y    0.831 :z    2.878}
    :H    {:e :H :x    1.729 :y   -1.148 :z    1.137}
    :H2   {:e :H :x    2.286 :y    0.078 :z    0.240}
    :HA   {:e :H :x    0.605 :y    1.537 :z    1.099}
    :HB2  {:e :H :x   -0.324 :y   -1.100 :z   -0.122}
    :HB3  {:e :H :x   -1.231 :y    0.431 :z   -0.144}
    :HG2  {:e :H :x    0.617 :y    1.508 :z   -1.398}
    :HG3  {:e :H :x    1.524 :y   -0.023 :z   -1.375}
    :HE21 {:e :H :x   -0.200 :y   -0.058 :z   -4.630}
    :HE22 {:e :H :x    1.109 :y    0.727 :z   -3.930}
    :HXT  {:e :H :x   -1.649 :y    0.556 :z    3.669}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG   :order 1  }
    {:a :CB   :b :HB2  :order 1  }
    {:a :CB   :b :HB3  :order 1  }
    {:a :CG   :b :CD   :order 1  }
    {:a :CG   :b :HG2  :order 1  }
    {:a :CG   :b :HG3  :order 1  }
    {:a :CD   :b :OE1  :order 2  }
    {:a :CD   :b :NE2  :order 1  }
    {:a :NE2  :b :HE21 :order 1  }
    {:a :NE2  :b :HE22 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; GLY -- glycine, C2 H5 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def GLY
  {:code "GLY"
   :formula "C2 H5 N O2"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.931 :y    0.090 :z   -0.034}
    :CA  {:e :C :x    0.761 :y   -0.799 :z   -0.008}
    :C   {:e :C :x   -0.498 :y    0.029 :z   -0.005}
    :O   {:e :O :x   -0.429 :y    1.235 :z   -0.023}
    :OXT {:e :O :x   -1.697 :y   -0.574 :z    0.018}
    :H   {:e :H :x    1.910 :y    0.738 :z    0.738}
    :H2  {:e :H :x    2.788 :y   -0.442 :z   -0.037}
    :HA2 {:e :H :x    0.772 :y   -1.440 :z   -0.889}
    :HA3 {:e :H :x    0.793 :y   -1.415 :z    0.891}
    :HXT {:e :H :x   -2.477 :y   -0.002 :z    0.019}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :HA2 :order 1  }
    {:a :CA  :b :HA3 :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; PRO -- proline, C5 H9 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def PRO
  {:code "PRO"
   :formula "C5 H9 N O2"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x   -0.816 :y    1.108 :z    0.254}
    :CA  {:e :C :x    0.001 :y   -0.107 :z    0.509}
    :C   {:e :C :x    1.408 :y    0.091 :z    0.005}
    :O   {:e :O :x    1.650 :y    0.980 :z   -0.777}
    :CB  {:e :C :x   -0.703 :y   -1.227 :z   -0.286}
    :CG  {:e :C :x   -2.163 :y   -0.753 :z   -0.439}
    :CD  {:e :C :x   -2.218 :y    0.614 :z    0.276}
    :OXT {:e :O :x    2.391 :y   -0.721 :z    0.424}
    :H   {:e :H :x   -0.707 :y    1.708 :z    1.057}
    :HA  {:e :H :x    0.009 :y   -0.343 :z    1.573}
    :HB2 {:e :H :x   -0.240 :y   -1.345 :z   -1.266}
    :HB3 {:e :H :x   -0.666 :y   -2.165 :z    0.267}
    :HG2 {:e :H :x   -2.416 :y   -0.638 :z   -1.493}
    :HG3 {:e :H :x   -2.843 :y   -1.458 :z    0.040}
    :HD2 {:e :H :x   -2.872 :y    1.300 :z   -0.263}
    :HD3 {:e :H :x   -2.559 :y    0.492 :z    1.304}
    :HXT {:e :H :x    3.293 :y   -0.594 :z    0.101}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :CD  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD  :order 1  }
    {:a :CG  :b :HG2 :order 1  }
    {:a :CG  :b :HG3 :order 1  }
    {:a :CD  :b :HD2 :order 1  }
    {:a :CD  :b :HD3 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; CYS -- cysteine, C3 H7 N O2 S, formal charge 0
;; ---------------------------------------------------------------------
(def CYS
  {:code "CYS"
   :formula "C3 H7 N O2 S"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.585 :y    0.483 :z   -0.081}
    :CA  {:e :C :x    0.141 :y    0.450 :z    0.186}
    :C   {:e :C :x   -0.095 :y    0.006 :z    1.606}
    :O   {:e :O :x    0.685 :y   -0.742 :z    2.143}
    :CB  {:e :C :x   -0.533 :y   -0.530 :z   -0.774}
    :SG  {:e :S :x   -0.247 :y    0.004 :z   -2.484}
    :OXT {:e :O :x   -1.174 :y    0.443 :z    2.275}
    :H   {:e :H :x    1.928 :y   -0.454 :z    0.063}
    :H2  {:e :H :x    1.693 :y    0.682 :z   -1.065}
    :HA  {:e :H :x   -0.277 :y    1.446 :z    0.042}
    :HB2 {:e :H :x   -0.114 :y   -1.526 :z   -0.630}
    :HB3 {:e :H :x   -1.604 :y   -0.554 :z   -0.575}
    :HG  {:e :H :x   -0.904 :y   -0.965 :z   -3.145}
    :HXT {:e :H :x   -1.326 :y    0.158 :z    3.186}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :SG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :SG  :b :HG  :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; SEC -- selenocysteine, C3 H7 N O2 Se, formal charge 0
;; ---------------------------------------------------------------------
(def SEC
  {:code "SEC"
   :formula "C3 H7 N O2 Se"
   :formal-charge 0
   :atoms
   {:N   {:e :N  :x   -0.783 :y    1.676 :z   -0.339}
    :CA  {:e :C  :x   -0.938 :y    0.217 :z   -0.405}
    :CB  {:e :C  :x    0.042 :y   -0.445 :z    0.565}
    :SE  {:e :SE :x    1.879 :y   -0.092 :z   -0.020}
    :C   {:e :C  :x   -2.349 :y   -0.156 :z   -0.027}
    :O   {:e :O  :x   -3.030 :y    0.619 :z    0.602}
    :OXT {:e :O  :x   -2.848 :y   -1.348 :z   -0.389}
    :H   {:e :H  :x   -1.373 :y    2.134 :z   -1.017}
    :H2  {:e :H  :x   -0.969 :y    2.018 :z    0.592}
    :HA  {:e :H  :x   -0.732 :y   -0.125 :z   -1.419}
    :HB2 {:e :H  :x   -0.105 :y   -0.037 :z    1.565}
    :HB3 {:e :H  :x   -0.134 :y   -1.521 :z    0.582}
    :HE  {:e :H  :x    2.691 :y   -0.839 :z    1.084}
    :HXT {:e :H  :x   -3.757 :y   -1.542 :z   -0.123}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :CB  :b :SE  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :SE  :b :HE  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; ALA -- alanine, C3 H7 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def ALA
  {:code "ALA"
   :formula "C3 H7 N O2"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x   -0.966 :y    0.493 :z    1.500}
    :CA  {:e :C :x    0.257 :y    0.418 :z    0.692}
    :C   {:e :C :x   -0.094 :y    0.017 :z   -0.716}
    :O   {:e :O :x   -1.056 :y   -0.682 :z   -0.923}
    :CB  {:e :C :x    1.204 :y   -0.620 :z    1.296}
    :OXT {:e :O :x    0.661 :y    0.439 :z   -1.742}
    :H   {:e :H :x   -1.383 :y   -0.425 :z    1.482}
    :H2  {:e :H :x   -0.676 :y    0.661 :z    2.452}
    :HA  {:e :H :x    0.746 :y    1.392 :z    0.682}
    :HB1 {:e :H :x    1.459 :y   -0.330 :z    2.316}
    :HB2 {:e :H :x    0.715 :y   -1.594 :z    1.307}
    :HB3 {:e :H :x    2.113 :y   -0.676 :z    0.697}
    :HXT {:e :H :x    0.435 :y    0.182 :z   -2.647}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :HB1 :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; VAL -- valine, C5 H11 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def VAL
  {:code "VAL"
   :formula "C5 H11 N O2"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x    1.564 :y   -0.642 :z    0.454}
    :CA   {:e :C :x    0.145 :y   -0.698 :z    0.079}
    :C    {:e :C :x   -0.037 :y   -0.093 :z   -1.288}
    :O    {:e :O :x    0.703 :y    0.784 :z   -1.664}
    :CB   {:e :C :x   -0.682 :y    0.086 :z    1.098}
    :CG1  {:e :C :x   -0.497 :y   -0.528 :z    2.487}
    :CG2  {:e :C :x   -0.218 :y    1.543 :z    1.119}
    :OXT  {:e :O :x   -1.022 :y   -0.529 :z   -2.089}
    :H    {:e :H :x    1.825 :y    0.332 :z    0.455}
    :H2   {:e :H :x    1.624 :y   -0.959 :z    1.410}
    :HA   {:e :H :x   -0.186 :y   -1.736 :z    0.064}
    :HB   {:e :H :x   -1.736 :y    0.044 :z    0.820}
    :HG11 {:e :H :x   -1.087 :y    0.031 :z    3.214}
    :HG12 {:e :H :x   -0.828 :y   -1.566 :z    2.472}
    :HG13 {:e :H :x    0.555 :y   -0.486 :z    2.765}
    :HG21 {:e :H :x    0.835 :y    1.585 :z    1.397}
    :HG22 {:e :H :x   -0.350 :y    1.981 :z    0.130}
    :HG23 {:e :H :x   -0.808 :y    2.103 :z    1.845}
    :HXT  {:e :H :x   -1.139 :y   -0.140 :z   -2.967}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG1  :order 1  }
    {:a :CB   :b :CG2  :order 1  }
    {:a :CB   :b :HB   :order 1  }
    {:a :CG1  :b :HG11 :order 1  }
    {:a :CG1  :b :HG12 :order 1  }
    {:a :CG1  :b :HG13 :order 1  }
    {:a :CG2  :b :HG21 :order 1  }
    {:a :CG2  :b :HG22 :order 1  }
    {:a :CG2  :b :HG23 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; LEU -- leucine, C6 H13 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def LEU
  {:code "LEU"
   :formula "C6 H13 N O2"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x   -1.661 :y    0.627 :z   -0.406}
    :CA   {:e :C :x   -0.205 :y    0.441 :z   -0.467}
    :C    {:e :C :x    0.180 :y   -0.055 :z   -1.836}
    :O    {:e :O :x   -0.591 :y   -0.731 :z   -2.474}
    :CB   {:e :C :x    0.221 :y   -0.583 :z    0.585}
    :CG   {:e :C :x   -0.170 :y   -0.079 :z    1.976}
    :CD1  {:e :C :x    0.256 :y   -1.104 :z    3.029}
    :CD2  {:e :C :x    0.526 :y    1.254 :z    2.250}
    :OXT  {:e :O :x    1.382 :y    0.254 :z   -2.348}
    :H    {:e :H :x   -2.077 :y   -0.272 :z   -0.592}
    :H2   {:e :H :x   -1.884 :y    0.858 :z    0.550}
    :HA   {:e :H :x    0.291 :y    1.391 :z   -0.271}
    :HB2  {:e :H :x    1.301 :y   -0.722 :z    0.540}
    :HB3  {:e :H :x   -0.275 :y   -1.534 :z    0.390}
    :HG   {:e :H :x   -1.250 :y    0.058 :z    2.021}
    :HD11 {:e :H :x   -0.022 :y   -0.745 :z    4.019}
    :HD12 {:e :H :x   -0.240 :y   -2.055 :z    2.833}
    :HD13 {:e :H :x    1.336 :y   -1.243 :z    2.984}
    :HD21 {:e :H :x    1.606 :y    1.115 :z    2.205}
    :HD22 {:e :H :x    0.222 :y    1.984 :z    1.500}
    :HD23 {:e :H :x    0.247 :y    1.613 :z    3.241}
    :HXT  {:e :H :x    1.630 :y   -0.064 :z   -3.226}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG   :order 1  }
    {:a :CB   :b :HB2  :order 1  }
    {:a :CB   :b :HB3  :order 1  }
    {:a :CG   :b :CD1  :order 1  }
    {:a :CG   :b :CD2  :order 1  }
    {:a :CG   :b :HG   :order 1  }
    {:a :CD1  :b :HD11 :order 1  }
    {:a :CD1  :b :HD12 :order 1  }
    {:a :CD1  :b :HD13 :order 1  }
    {:a :CD2  :b :HD21 :order 1  }
    {:a :CD2  :b :HD22 :order 1  }
    {:a :CD2  :b :HD23 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; ILE -- isoleucine, C6 H13 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def ILE
  {:code "ILE"
   :formula "C6 H13 N O2"
   :formal-charge 0
   :atoms
   {:N    {:e :N :x   -1.944 :y    0.335 :z   -0.343}
    :CA   {:e :C :x   -0.487 :y    0.519 :z   -0.369}
    :C    {:e :C :x    0.066 :y   -0.032 :z   -1.657}
    :O    {:e :O :x   -0.484 :y   -0.958 :z   -2.203}
    :CB   {:e :C :x    0.140 :y   -0.219 :z    0.814}
    :CG1  {:e :C :x   -0.421 :y    0.341 :z    2.122}
    :CG2  {:e :C :x    1.658 :y   -0.027 :z    0.788}
    :CD1  {:e :C :x    0.206 :y   -0.397 :z    3.305}
    :OXT  {:e :O :x    1.171 :y    0.504 :z   -2.197}
    :H    {:e :H :x   -2.112 :y   -0.656 :z   -0.410}
    :H2   {:e :H :x   -2.256 :y    0.622 :z    0.572}
    :HA   {:e :H :x   -0.253 :y    1.582 :z   -0.299}
    :HB   {:e :H :x   -0.092 :y   -1.281 :z    0.744}
    :HG12 {:e :H :x   -1.502 :y    0.204 :z    2.141}
    :HG13 {:e :H :x   -0.188 :y    1.403 :z    2.192}
    :HG21 {:e :H :x    1.891 :y    1.034 :z    0.857}
    :HG22 {:e :H :x    2.105 :y   -0.554 :z    1.631}
    :HG23 {:e :H :x    2.059 :y   -0.427 :z   -0.143}
    :HD11 {:e :H :x   -0.193 :y    0.001 :z    4.237}
    :HD12 {:e :H :x   -0.026 :y   -1.460 :z    3.235}
    :HD13 {:e :H :x    1.287 :y   -0.261 :z    3.286}
    :HXT  {:e :H :x    1.527 :y    0.150 :z   -3.024}}
   :bonds
   [{:a :N    :b :CA   :order 1  }
    {:a :N    :b :H    :order 1  }
    {:a :N    :b :H2   :order 1  }
    {:a :CA   :b :C    :order 1  }
    {:a :CA   :b :CB   :order 1  }
    {:a :CA   :b :HA   :order 1  }
    {:a :C    :b :O    :order 2  }
    {:a :C    :b :OXT  :order 1  }
    {:a :CB   :b :CG1  :order 1  }
    {:a :CB   :b :CG2  :order 1  }
    {:a :CB   :b :HB   :order 1  }
    {:a :CG1  :b :CD1  :order 1  }
    {:a :CG1  :b :HG12 :order 1  }
    {:a :CG1  :b :HG13 :order 1  }
    {:a :CG2  :b :HG21 :order 1  }
    {:a :CG2  :b :HG22 :order 1  }
    {:a :CG2  :b :HG23 :order 1  }
    {:a :CD1  :b :HD11 :order 1  }
    {:a :CD1  :b :HD12 :order 1  }
    {:a :CD1  :b :HD13 :order 1  }
    {:a :OXT  :b :HXT  :order 1  }]})

;; ---------------------------------------------------------------------
;; MET -- methionine, C5 H11 N O2 S, formal charge 0
;; ---------------------------------------------------------------------
(def MET
  {:code "MET"
   :formula "C5 H11 N O2 S"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x   -1.816 :y    0.142 :z   -1.166}
    :CA  {:e :C :x   -0.392 :y    0.499 :z   -1.214}
    :C   {:e :C :x    0.206 :y    0.002 :z   -2.504}
    :O   {:e :O :x   -0.236 :y   -0.989 :z   -3.033}
    :CB  {:e :C :x    0.334 :y   -0.145 :z   -0.032}
    :CG  {:e :C :x   -0.273 :y    0.359 :z    1.277}
    :SD  {:e :S :x    0.589 :y   -0.405 :z    2.678}
    :CE  {:e :C :x   -0.314 :y    0.353 :z    4.056}
    :OXT {:e :O :x    1.232 :y    0.661 :z   -3.066}
    :H   {:e :H :x   -1.865 :y   -0.864 :z   -1.220}
    :H2  {:e :H :x   -2.149 :y    0.399 :z   -0.248}
    :HA  {:e :H :x   -0.287 :y    1.582 :z   -1.158}
    :HB2 {:e :H :x    1.391 :y    0.119 :z   -0.068}
    :HB3 {:e :H :x    0.229 :y   -1.229 :z   -0.088}
    :HG2 {:e :H :x   -1.330 :y    0.094 :z    1.313}
    :HG3 {:e :H :x   -0.168 :y    1.442 :z    1.333}
    :HE1 {:e :H :x    0.090 :y   -0.010 :z    5.000}
    :HE2 {:e :H :x   -0.207 :y    1.436 :z    4.008}
    :HE3 {:e :H :x   -1.369 :y    0.088 :z    3.988}
    :HXT {:e :H :x    1.616 :y    0.342 :z   -3.894}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :SD  :order 1  }
    {:a :CG  :b :HG2 :order 1  }
    {:a :CG  :b :HG3 :order 1  }
    {:a :SD  :b :CE  :order 1  }
    {:a :CE  :b :HE1 :order 1  }
    {:a :CE  :b :HE2 :order 1  }
    {:a :CE  :b :HE3 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; PHE -- phenylalanine, C9 H11 N O2, formal charge 0
;; ---------------------------------------------------------------------
(def PHE
  {:code "PHE"
   :formula "C9 H11 N O2"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.317 :y    0.962 :z    1.014}
    :CA  {:e :C :x   -0.020 :y    0.426 :z    1.300}
    :C   {:e :C :x   -0.109 :y    0.047 :z    2.756}
    :O   {:e :O :x    0.879 :y   -0.317 :z    3.346}
    :CB  {:e :C :x   -0.270 :y   -0.809 :z    0.434}
    :CG  {:e :C :x   -0.181 :y   -0.430 :z   -1.020}
    :CD1 {:e :C :x    1.031 :y   -0.498 :z   -1.680}
    :CD2 {:e :C :x   -1.314 :y   -0.018 :z   -1.698}
    :CE1 {:e :C :x    1.112 :y   -0.150 :z   -3.015}
    :CE2 {:e :C :x   -1.231 :y    0.333 :z   -3.032}
    :CZ  {:e :C :x   -0.018 :y    0.265 :z   -3.691}
    :OXT {:e :O :x   -1.286 :y    0.113 :z    3.396}
    :H   {:e :H :x    1.975 :y    0.230 :z    1.235}
    :H2  {:e :H :x    1.365 :y    1.104 :z    0.017}
    :HA  {:e :H :x   -0.770 :y    1.184 :z    1.076}
    :HB2 {:e :H :x    0.480 :y   -1.568 :z    0.659}
    :HB3 {:e :H :x   -1.262 :y   -1.207 :z    0.646}
    :HD1 {:e :H :x    1.915 :y   -0.824 :z   -1.152}
    :HD2 {:e :H :x   -2.262 :y    0.034 :z   -1.183}
    :HE1 {:e :H :x    2.060 :y   -0.203 :z   -3.530}
    :HE2 {:e :H :x   -2.116 :y    0.659 :z   -3.560}
    :HZ  {:e :H :x    0.045 :y    0.538 :z   -4.734}
    :HXT {:e :H :x   -1.343 :y   -0.130 :z    4.330}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD1 :order 1.5}  ; aromatic: file says DOUB
    {:a :CG  :b :CD2 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :CE1 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :HD1 :order 1  }
    {:a :CD2 :b :CE2 :order 1.5}  ; aromatic: file says DOUB
    {:a :CD2 :b :HD2 :order 1  }
    {:a :CE1 :b :CZ  :order 1.5}  ; aromatic: file says DOUB
    {:a :CE1 :b :HE1 :order 1  }
    {:a :CE2 :b :CZ  :order 1.5}  ; aromatic: file says SING
    {:a :CE2 :b :HE2 :order 1  }
    {:a :CZ  :b :HZ  :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; TYR -- tyrosine, C9 H11 N O3, formal charge 0
;; ---------------------------------------------------------------------
(def TYR
  {:code "TYR"
   :formula "C9 H11 N O3"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.320 :y    0.952 :z    1.428}
    :CA  {:e :C :x   -0.018 :y    0.429 :z    1.734}
    :C   {:e :C :x   -0.103 :y    0.094 :z    3.201}
    :O   {:e :O :x    0.886 :y   -0.254 :z    3.799}
    :CB  {:e :C :x   -0.274 :y   -0.831 :z    0.907}
    :CG  {:e :C :x   -0.189 :y   -0.496 :z   -0.559}
    :CD1 {:e :C :x    1.022 :y   -0.589 :z   -1.219}
    :CD2 {:e :C :x   -1.324 :y   -0.102 :z   -1.244}
    :CE1 {:e :C :x    1.103 :y   -0.282 :z   -2.563}
    :CE2 {:e :C :x   -1.247 :y    0.210 :z   -2.587}
    :CZ  {:e :C :x   -0.032 :y    0.118 :z   -3.252}
    :OH  {:e :O :x    0.044 :y    0.420 :z   -4.574}
    :OXT {:e :O :x   -1.279 :y    0.184 :z    3.842}
    :H   {:e :H :x    1.977 :y    0.225 :z    1.669}
    :H2  {:e :H :x    1.365 :y    1.063 :z    0.426}
    :HA  {:e :H :x   -0.767 :y    1.183 :z    1.489}
    :HB2 {:e :H :x    0.473 :y   -1.585 :z    1.152}
    :HB3 {:e :H :x   -1.268 :y   -1.219 :z    1.134}
    :HD1 {:e :H :x    1.905 :y   -0.902 :z   -0.683}
    :HD2 {:e :H :x   -2.269 :y   -0.031 :z   -0.727}
    :HE1 {:e :H :x    2.049 :y   -0.354 :z   -3.078}
    :HE2 {:e :H :x   -2.132 :y    0.523 :z   -3.121}
    :HH  {:e :H :x   -0.123 :y   -0.399 :z   -5.059}
    :HXT {:e :H :x   -1.333 :y   -0.030 :z    4.784}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD1 :order 1.5}  ; aromatic: file says DOUB
    {:a :CG  :b :CD2 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :CE1 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :HD1 :order 1  }
    {:a :CD2 :b :CE2 :order 1.5}  ; aromatic: file says DOUB
    {:a :CD2 :b :HD2 :order 1  }
    {:a :CE1 :b :CZ  :order 1.5}  ; aromatic: file says DOUB
    {:a :CE1 :b :HE1 :order 1  }
    {:a :CE2 :b :CZ  :order 1.5}  ; aromatic: file says SING
    {:a :CE2 :b :HE2 :order 1  }
    {:a :CZ  :b :OH  :order 1  }
    {:a :OH  :b :HH  :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; ---------------------------------------------------------------------
;; TRP -- tryptophan, C11 H12 N2 O2, formal charge 0
;; ---------------------------------------------------------------------
(def TRP
  {:code "TRP"
   :formula "C11 H12 N2 O2"
   :formal-charge 0
   :atoms
   {:N   {:e :N :x    1.278 :y    1.121 :z    2.059}
    :CA  {:e :C :x   -0.008 :y    0.417 :z    1.970}
    :C   {:e :C :x   -0.490 :y    0.076 :z    3.357}
    :O   {:e :O :x    0.308 :y   -0.130 :z    4.240}
    :CB  {:e :C :x    0.168 :y   -0.868 :z    1.161}
    :CG  {:e :C :x    0.650 :y   -0.526 :z   -0.225}
    :CD1 {:e :C :x    1.928 :y   -0.418 :z   -0.622}
    :CD2 {:e :C :x   -0.186 :y   -0.256 :z   -1.396}
    :NE1 {:e :N :x    1.978 :y   -0.095 :z   -1.951}
    :CE2 {:e :C :x    0.701 :y    0.014 :z   -2.454}
    :CE3 {:e :C :x   -1.564 :y   -0.210 :z   -1.615}
    :CZ2 {:e :C :x    0.190 :y    0.314 :z   -3.712}
    :CZ3 {:e :C :x   -2.044 :y    0.086 :z   -2.859}
    :CH2 {:e :C :x   -1.173 :y    0.348 :z   -3.907}
    :OXT {:e :O :x   -1.806 :y    0.001 :z    3.610}
    :H   {:e :H :x    1.921 :y    0.493 :z    2.518}
    :H2  {:e :H :x    1.611 :y    1.237 :z    1.113}
    :HA  {:e :H :x   -0.740 :y    1.058 :z    1.479}
    :HB2 {:e :H :x    0.900 :y   -1.509 :z    1.652}
    :HB3 {:e :H :x   -0.786 :y   -1.390 :z    1.095}
    :HD1 {:e :H :x    2.789 :y   -0.564 :z    0.012}
    :HE1 {:e :H :x    2.791 :y    0.036 :z   -2.462}
    :HE3 {:e :H :x   -2.248 :y   -0.413 :z   -0.804}
    :HZ2 {:e :H :x    0.860 :y    0.521 :z   -4.534}
    :HZ3 {:e :H :x   -3.110 :y    0.116 :z   -3.029}
    :HH2 {:e :H :x   -1.567 :y    0.582 :z   -4.885}
    :HXT {:e :H :x   -2.115 :y   -0.217 :z    4.500}}
   :bonds
   [{:a :N   :b :CA  :order 1  }
    {:a :N   :b :H   :order 1  }
    {:a :N   :b :H2  :order 1  }
    {:a :CA  :b :C   :order 1  }
    {:a :CA  :b :CB  :order 1  }
    {:a :CA  :b :HA  :order 1  }
    {:a :C   :b :O   :order 2  }
    {:a :C   :b :OXT :order 1  }
    {:a :CB  :b :CG  :order 1  }
    {:a :CB  :b :HB2 :order 1  }
    {:a :CB  :b :HB3 :order 1  }
    {:a :CG  :b :CD1 :order 1.5}  ; aromatic: file says DOUB
    {:a :CG  :b :CD2 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :NE1 :order 1.5}  ; aromatic: file says SING
    {:a :CD1 :b :HD1 :order 1  }
    {:a :CD2 :b :CE2 :order 1.5}  ; aromatic: file says DOUB
    {:a :CD2 :b :CE3 :order 1.5}  ; aromatic: file says SING
    {:a :NE1 :b :CE2 :order 1.5}  ; aromatic: file says SING
    {:a :NE1 :b :HE1 :order 1  }
    {:a :CE2 :b :CZ2 :order 1.5}  ; aromatic: file says SING
    {:a :CE3 :b :CZ3 :order 1.5}  ; aromatic: file says DOUB
    {:a :CE3 :b :HE3 :order 1  }
    {:a :CZ2 :b :CH2 :order 1.5}  ; aromatic: file says DOUB
    {:a :CZ2 :b :HZ2 :order 1  }
    {:a :CZ3 :b :CH2 :order 1.5}  ; aromatic: file says SING
    {:a :CZ3 :b :HZ3 :order 1  }
    {:a :CH2 :b :HH2 :order 1  }
    {:a :OXT :b :HXT :order 1  }]})

;; Every residue, keyed by its three-letter code, in teaching order.
(def all
  {:ASP ASP
   :GLU GLU
   :LYS LYS
   :ARG ARG
   :HIS HIS
   :SER SER
   :THR THR
   :ASN ASN
   :GLN GLN
   :GLY GLY
   :PRO PRO
   :CYS CYS
   :SEC SEC
   :ALA ALA
   :VAL VAL
   :LEU LEU
   :ILE ILE
   :MET MET
   :PHE PHE
   :TYR TYR
   :TRP TRP})
