(ns nanoverse.amino.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.amino.geometry :as geo]))

;; Babylon rendering for the amino slide. Same pH knob as the carboxyl slide,
;; deliberately -- what differs is which way the molecule moves when you turn
;; it, and that the readout to watch is TAKES rather than GIVES.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.4) :beta 1.2 :radius 8.4})
        groups (mapv #(scene/state-group! ctx %) geo/states)
        st (scene/controls! prefix)
        active (scene/titration!
                 prefix st groups geo/pkas
                 (fn [ph step]
                   (let [state (:state (nth groups step))
                         ;; fraction still holding the proton -- the readout
                         ;; people actually want for a base
                         protonated (- 1 (sol/fraction-deprotonated ph geo/PKA))]
                     (scene/set-text! prefix "species" (:label state))
                     (scene/set-text! prefix "protonated" (str (.toFixed (* 100 protonated) 2) " %"))
                     (scene/set-text! prefix "charge" (scene/signed (:charge state)))
                     (scene/set-text! prefix "lp" (if (zero? (:charge state)) "1 (free)" "0 (spent)")))))]

    (letfn [(frame []
              (let [{:keys [amplitude tumble show-bent]} @st
                    t (scene/tick! st)
                    rs (scene/update! ctx @active {:t t :amp amplitude :tumble tumble
                                                   :show-bent show-bent})]
                (scene/set-num! prefix "count" (scene/count-lit rs))
                (scene/set-num! prefix "gives"
                                (count (filter #(and (:lit %) (= (:kind %) :molecule-donates)) rs)))
                (scene/set-num! prefix "takes"
                                (count (filter #(and (:lit %) (= (:kind %) :water-donates)) rs)))
                (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

      (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))
