(ns nanoverse.main
  (:require [nanoverse.deck :as deck]
            [nanoverse.hydroxyl.babylon-core :as hydroxyl]
            [nanoverse.aldehyde.babylon-core :as aldehyde]
            [nanoverse.ketone.babylon-core :as ketone]
            [nanoverse.carboxyl.babylon-core :as carboxyl]
            [nanoverse.amino.babylon-core :as amino]
            [nanoverse.phosphate.babylon-core :as phosphate]
            [nanoverse.sulfhydryl.babylon-core :as sulfhydryl]
            [nanoverse.methyl.babylon-core :as methyl]))

;; Entry point for the whole deck. This is the only namespace that knows both
;; the deck machinery and the individual slides, which is what keeps
;; nanoverse.deck free of a require cycle back to the slides.
;;
;; Slide order is the teaching order, and each slide is built to be read
;; against the one before it:
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

(deck/mount!
  [{:section-id "slide-hydroxyl"
    :prefix "oh-"
    :label "Hydroxyl in Water — ethanol"
    :build hydroxyl/build}
   {:section-id "slide-aldehyde"
    :prefix "ald-"
    :label "Aldehyde in Water — acetaldehyde"
    :build aldehyde/build}
   {:section-id "slide-ketone"
    :prefix "ket-"
    :label "Keto in Water — acetone"
    :build ketone/build}
   {:section-id "slide-carboxyl"
    :prefix "cbx-"
    :label "Carboxyl in Water — acetic acid"
    :build carboxyl/build}
   {:section-id "slide-amino"
    :prefix "amn-"
    :label "Amino in Water — methylamine"
    :build amino/build}
   {:section-id "slide-phosphate"
    :prefix "pho-"
    :label "Phosphate in Water — 3-phosphoglycerate"
    :build phosphate/build}
   {:section-id "slide-sulfhydryl"
    :prefix "sh-"
    :label "Sulfhydryl in Water — mercaptoethanol"
    :build sulfhydryl/build}
   {:section-id "slide-methyl"
    :prefix "me-"
    :label "Methyl in Water — alanine"
    :build methyl/build}])
