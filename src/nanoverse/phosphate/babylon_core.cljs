(ns nanoverse.phosphate.babylon-core
  (:require [clojure.string :as string]
            [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.phosphate.geometry :as geo]))

;; Babylon rendering for the phosphate slide. Same pH knob as the two slides
;; before it, but with three rungs instead of one -- and the readout to watch
;; is the net charge, which is what a phosphate group is actually for.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.9) :beta 1.30 :radius 11.4
                                :lower 5 :upper 30})
        groups (mapv #(scene/state-group! ctx %) geo/states)
        st (scene/controls! prefix)
        active (scene/titration!
                 prefix st groups geo/pkas
                 (fn [ph step]
                   (let [state (:state (nth groups step))
                         released (:released state)]
                     (scene/set-text! prefix "species" (:label state))
                     (scene/set-text! prefix "charge" (sol/signed-charge (:charge state)))
                     (scene/set-text! prefix "released"
                       (if (seq released) (string/join " · " released) "none"))
                     ;; how sharp the majority-species picture actually is: at
                     ;; a pH sitting on a pKa it is a coin flip, and saying so
                     ;; is more honest than drawing one molecule confidently
                     (scene/set-text! prefix "mix"
                       (let [nearest (reduce (fn [a b] (if (< (js/Math.abs (- ph b))
                                                              (js/Math.abs (- ph a))) b a))
                                             (first geo/pkas) geo/pkas)
                             f (sol/fraction-deprotonated ph nearest)
                             maj (js/Math.max f (- 1 f))]
                         (str (.toFixed (* 100 maj) 0) " %")))
                     (scene/set-text! prefix "cell"
                       (if (>= ph 6.21) "yes — this is the cell's form" "no")))))]

    (letfn [(frame []
              (let [{:keys [amplitude tumble show-bent]} @st
                    t (scene/tick! st)
                    rs (scene/update! ctx @active {:t t :amp amplitude :tumble tumble
                                                   :show-bent show-bent})]
                (scene/set-num! prefix "count" (scene/count-lit rs))
                (scene/set-num! prefix "phos" (scene/count-lit rs :phosphate))
                (scene/set-num! prefix "carb" (scene/count-lit rs :carboxyl))
                (scene/set-num! prefix "hydroxyl" (scene/count-lit rs :hydroxyl))
                (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

      (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))
