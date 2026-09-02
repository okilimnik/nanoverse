(ns nanoverse.aldehyde.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.aldehyde.geometry :as geo]))

;; Babylon rendering for the aldehyde slide. All chemistry lives in
;; geometry.cljs and everything generic in nanoverse.scene; what is left here
;; is this slide's readouts.
;;
;; This is the slide that introduces the tumble knob, and with it the first
;; contacts that are close enough to bond and rejected anyway for pointing the
;; wrong way. Those are drawn dull red by scene/update! whenever the
;; show-bent toggle is on.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.7) :beta 1.2 :radius 9.0
                                :lower 4 :upper 22})
        group (scene/state-group! ctx geo/state)
        st (scene/controls! prefix)]

    (letfn [(frame []
              (let [{:keys [amplitude tumble show-bent]} @st
                    t (scene/tick! st)
                    rs (scene/update! ctx group {:t t :amp amplitude :tumble tumble
                                                 :show-bent show-bent})]
                (scene/set-num! prefix "count" (scene/count-lit rs))
                ;; kept as a live count rather than a hardcoded 0, so the
                ;; readout measures the same thing the hydroxyl slide measures
                ;; -- it stays at zero because geometry.cljs declares no
                ;; donors, not because anything special-cases it here
                (scene/set-num! prefix "gives"
                                (count (filter #(and (:lit %) (= (:kind %) :molecule-donates)) rs)))
                (scene/set-num! prefix "takes"
                                (count (filter #(and (:lit %) (= (:kind %) :water-donates)) rs)))
                (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

      (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))
