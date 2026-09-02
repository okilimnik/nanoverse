(ns nanoverse.methyl.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.methyl.geometry :as geo]))

;; Babylon rendering for the methyl slide. The pH knob is here so the amino
;; and carboxyl ends visibly change while the methyl visibly does not; the
;; slide's own knob shades the side chain so you can find it while the rest of
;; the molecule is busy.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.7) :beta 1.25 :radius 10.5
                                :lower 5 :upper 26})
        groups (mapv #(scene/state-group! ctx %) geo/states)
        st (scene/controls! prefix)
        phobic-toggle (deck/el prefix "phobic")
        phobic-keys [:CB :HB1 :HB2 :HB3]]

    (scene/set-text! prefix "phobic-waters" geo/methyl-water-count)

    (letfn [(paint-phobic! [on?]
              (doseq [g groups]
                (doseq [k phobic-keys]
                  (when-let [m (get (:atom-meshes (:mol g)) k)]
                    (set! (.-material m)
                          (if on?
                            (:phobic (:materials ctx))
                            (get (:materials ctx) (:e (get (:atoms (:state g)) k)))))))))]

      (when phobic-toggle
        (paint-phobic! (.-checked phobic-toggle))
        (.addEventListener phobic-toggle "change"
          (fn [] (paint-phobic! (.-checked phobic-toggle)))))

      (let [active (scene/titration!
                     prefix st groups geo/pkas
                     (fn [_ph step]
                       (let [state (:state (nth groups step))]
                         (scene/set-text! prefix "species" (:label state))
                         (scene/set-text! prefix "charge" (sol/signed-charge (:charge state)))
                         ;; net zero and "no charges" are very different things,
                         ;; and the zwitterion is the reason to say so
                         (scene/set-text! prefix "ends"
                           (case step
                             0 "+1 amino, 0 carboxyl"
                             1 "+1 amino, −1 carboxyl"
                             "0 amino, −1 carboxyl")))))]

        (letfn [(frame []
                  (let [{:keys [amplitude tumble show-bent]} @st
                        t (scene/tick! st)
                        rs (scene/update! ctx @active {:t t :amp amplitude :tumble tumble
                                                       :show-bent show-bent})]
                    (scene/set-num! prefix "count" (scene/count-lit rs))
                    (scene/set-num! prefix "amino" (scene/count-lit rs :amino))
                    (scene/set-num! prefix "carb" (scene/count-lit rs :carboxyl))
                    ;; the whole slide, in one readout that never moves
                    (scene/set-num! prefix "methyl" (scene/count-lit rs :methyl))
                    (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

          (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))))
