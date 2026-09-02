(ns nanoverse.hydroxyl.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.hydroxyl.geometry :as geo]))

;; Babylon rendering for the hydroxyl (ethanol) slide. All chemistry lives in
;; geometry.cljs and everything generic lives in nanoverse.scene; what is left
;; here is this slide's readouts.
;;
;; This is the first slide in the deck and the only one with no second state
;; and no tumble control -- scene/controls! simply finds no such elements in
;; the DOM and leaves the tumble at zero. That is deliberate: the waters here
;; translate but never rotate, which is why the angle half of the bond test
;; never fires on this slide and the next one has to introduce it.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.5) :beta 1.15 :radius 8.5
                                :lower 4 :upper 20})
        group (scene/state-group! ctx geo/state)
        st (scene/controls! prefix)]

    (letfn [(frame []
              (let [{:keys [amplitude tumble]} @st
                    t (scene/tick! st)
                    rs (scene/update! ctx group {:t t :amp amplitude :tumble tumble})]
                (scene/set-num! prefix "count" (scene/count-lit rs))
                ;; split by which side of the -OH is doing the work: ethanol's
                ;; own hydroxyl hydrogen donating outward, versus its oxygen
                ;; accepting a water's hydrogen in
                (scene/set-num! prefix "gives"
                                (count (filter #(and (:lit %) (= (:kind %) :molecule-donates)) rs)))
                (scene/set-num! prefix "takes"
                                (count (filter #(and (:lit %) (= (:kind %) :water-donates)) rs)))
                ;; nearest approach across every candidate, bonded or not --
                ;; the one readout that keeps moving when the count does not
                (let [nearest (reduce (fn [a r] (js/Math.min a (:dist r))) js/Infinity rs)]
                  (scene/set-text! prefix "dist"
                                   (if (js/isFinite nearest) (str (.toFixed nearest 2) " Å") "—")))))]

      (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))
