(ns nanoverse.main
  (:require [nanoverse.deck :as deck]
            [nanoverse.hydroxyl.babylon-core :as hydroxyl]
            [nanoverse.aldehyde.babylon-core :as aldehyde]))

;; Entry point for the whole deck. This is the only namespace that knows both
;; the deck machinery and the individual slides, which is what keeps
;; nanoverse.deck free of a require cycle back to the slides.
;;
;; Slide order is the teaching order: the hydroxyl first (an -OH that both
;; donates and accepts), then the aldehyde (a C=O that can only accept). The
;; second only lands as a contrast if the first came before it.

(deck/mount!
  [{:section-id "slide-hydroxyl"
    :prefix "oh-"
    :label "Hydroxyl in Water — ethanol"
    :build hydroxyl/build}
   {:section-id "slide-aldehyde"
    :prefix "ald-"
    :label "Aldehyde in Water — D-glyceraldehyde"
    :build aldehyde/build}])
