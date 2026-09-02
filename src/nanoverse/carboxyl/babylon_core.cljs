(ns nanoverse.carboxyl.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.carboxyl.geometry :as geo]))

;; Babylon rendering for the carboxyl slide. The knob here is pH: it crosses a
;; real pKa and swaps one deposited structure for another.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.5) :beta 1.15 :radius 9.0})
        groups (mapv #(scene/state-group! ctx %) geo/states)
        st (scene/controls! prefix)
        active (scene/titration!
                 prefix st groups geo/pkas
                 (fn [ph step]
                   (let [f (sol/fraction-deprotonated ph geo/PKA)
                         state (:state (nth groups step))
                         [d1 d2] (geo/co-lengths (:atoms state))]
                     (scene/set-text! prefix "species" (:label state))
                     (scene/set-text! prefix "ionized" (str (.toFixed (* 100 f) 1) " %"))
                     (scene/set-text! prefix "charge" (scene/signed (:charge state)))
                     ;; the whole resonance story, read live off the two files
                     (scene/set-text! prefix "co"
                       (str (.toFixed d1 3) " / " (.toFixed d2 3) " Å"))
                     (scene/set-text! prefix "co-note"
                       (if (< (js/Math.abs (- d1 d2)) 0.01) "identical" "one double, one single")))))]

    (letfn [(frame []
              (let [{:keys [amplitude tumble show-bent]} @st
                    t (scene/tick! st)
                    g @active
                    rs (scene/update! ctx g {:t t :amp amplitude :tumble tumble
                                             :show-bent show-bent})]
                (scene/set-num! prefix "count" (scene/count-lit rs))
                (scene/set-num! prefix "gives"
                                (count (filter #(and (:lit %) (= (:kind %) :molecule-donates)) rs)))
                (scene/set-num! prefix "takes"
                                (count (filter #(and (:lit %) (= (:kind %) :water-donates)) rs)))
                (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

      (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))
