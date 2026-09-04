(ns nanoverse.main
  (:require [nanoverse.deck :as deck]
            [nanoverse.hydroxyl.babylon-core :as hydroxyl]
            [nanoverse.aldehyde.babylon-core :as aldehyde]
            [nanoverse.ketone.babylon-core :as ketone]
            [nanoverse.carboxyl.babylon-core :as carboxyl]
            [nanoverse.amino.babylon-core :as amino]
            [nanoverse.phosphate.babylon-core :as phosphate]
            [nanoverse.sulfhydryl.babylon-core :as sulfhydryl]
            [nanoverse.methyl.babylon-core :as methyl]
            [nanoverse.aminoacid.babylon-core :as aa]))

;; Entry point for the whole deck. This is the only namespace that knows both
;; the deck machinery and the individual slides, which is what keeps
;; nanoverse.deck free of a require cycle back to the slides.
;;
;; ---------------------------------------------------------------------
;; CHAPTER 1 -- Functional groups. One group at a time, in water.
;;
;;   1 hydroxyl     an -OH gives AND takes
;;   2 aldehyde     swap it for a -CHO and it can only take
;;   3 ketone       move that same C=O to the middle of the chain
;;   4 carboxyl     put an -OH on the carbonyl and the group starts IONISING
;;   5 amino        the same pH slider, run in the opposite direction
;;   6 phosphate    three ionisable protons at once: charge as a design tool
;;   7 sulfhydryl   swap oxygen for the sulfur underneath it in the table
;;   8 methyl       a group that does nothing at all, and why that matters
;;
;; Slides 1-3 are about what a group can do with water; 4-6 about what pH does
;; to it; 7-8 about what the element itself decides.
;;
;; ---------------------------------------------------------------------
;; CHAPTER 2 -- Amino acids. The same groups, now bolted onto one shared
;; backbone, which is how a cell actually uses them. Four slides, one per
;; class of the standard textbook chart, in the order that makes the argument:
;;
;;   1 charged      the two acids and the three bases, with a pH slider
;;   2 polar        the ones that hydrogen bond but never ionise
;;   3 special      no side chain, a side chain tied in a knot, and sulfur
;;   4 nonpolar     the greasy ones -- including two that hydrogen bond anyway
;;
;; The last slide is where the classification the chapter is organised by
;; visibly stops working, which is the point of putting it last.
;; ---------------------------------------------------------------------

(deck/mount!
  [{:title "Functional groups"
    :blurb "What water does around one group"
    :slides
    [{:section-id "slide-hydroxyl"
      :prefix "oh-"
      :nav "Hydroxyl"
      :label "Hydroxyl in Water — ethanol"
      :build hydroxyl/build}
     {:section-id "slide-aldehyde"
      :prefix "ald-"
      :nav "Aldehyde"
      :label "Aldehyde in Water — acetaldehyde"
      :build aldehyde/build}
     {:section-id "slide-ketone"
      :prefix "ket-"
      :nav "Keto"
      :label "Keto in Water — acetone"
      :build ketone/build}
     {:section-id "slide-carboxyl"
      :prefix "cbx-"
      :nav "Carboxyl"
      :label "Carboxyl in Water — acetic acid"
      :build carboxyl/build}
     {:section-id "slide-amino"
      :prefix "amn-"
      :nav "Amino"
      :label "Amino in Water — methylamine"
      :build amino/build}
     {:section-id "slide-phosphate"
      :prefix "pho-"
      :nav "Phosphate"
      :label "Phosphate in Water — 3-phosphoglycerate"
      :build phosphate/build}
     {:section-id "slide-sulfhydryl"
      :prefix "sh-"
      :nav "Sulfhydryl"
      :label "Sulfhydryl in Water — mercaptoethanol"
      :build sulfhydryl/build}
     {:section-id "slide-methyl"
      :prefix "me-"
      :nav "Methyl"
      :label "Methyl in Water — alanine"
      :build methyl/build}]}

   {:title "Amino acids"
    :blurb "Twenty-one side chains, one backbone"
    :slides
    [{:section-id "slide-aa-charged"
      :prefix "aac-"
      :nav "Charged side chains"
      :label "Charged Side Chains — Asp, Glu, Lys, Arg, His"
      :build aa/build-charged}
     {:section-id "slide-aa-polar"
      :prefix "aap-"
      :nav "Polar, uncharged"
      :label "Polar Uncharged Side Chains — Ser, Thr, Asn, Gln"
      :build aa/build-polar}
     {:section-id "slide-aa-special"
      :prefix "aas-"
      :nav "Special cases"
      :label "Special Cases — Gly, Pro, Cys, Sec"
      :build aa/build-special}
     {:section-id "slide-aa-nonpolar"
      :prefix "aan-"
      :nav "Nonpolar, hydrophobic"
      :label "Nonpolar Side Chains — Ala, Val, Leu, Ile, Met, Phe, Tyr, Trp"
      :build aa/build-nonpolar}]}])
