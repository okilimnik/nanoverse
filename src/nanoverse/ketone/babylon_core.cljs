(ns nanoverse.ketone.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.ketone.geometry :as geo]))

;; Babylon rendering for the ketone slide. The chemistry is all in
;; geometry.cljs and the generic scene machinery is in nanoverse.scene; what
;; is left here is this slide's own knob -- shading the greasy half of the
;; molecule -- and its own readouts.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.6) :beta 1.18 :radius 9.2})
        group (scene/state-group! ctx geo/state)
        st (scene/controls! prefix)
        phobic-toggle (deck/el prefix "phobic")
        ;; the atoms that make up the two methyl groups: everything bonded to
        ;; a methyl carbon, plus the carbon itself
        phobic-keys (concat [:C1 :C2]
                            (sol/neighbours geo/bonds :C1)
                            (sol/neighbours geo/bonds :C2))]

    (scene/set-text! prefix "co" (str (.toFixed geo/co-length 3) " Å"))
    (scene/set-text! prefix "phobic-waters" geo/methyl-water-count)

    (letfn [(paint-phobic! [on?]
              (doseq [k phobic-keys]
                (when-let [m (get (:atom-meshes (:mol group)) k)]
                  (when-not (= k :C)
                    (set! (.-material m)
                          (if on?
                            (:phobic (:materials ctx))
                            (get (:materials ctx) (:e (get geo/atoms k)))))))))]

      (when phobic-toggle
        (paint-phobic! (.-checked phobic-toggle))
        (.addEventListener phobic-toggle "change"
          (fn [] (paint-phobic! (.-checked phobic-toggle)))))

      (letfn [(frame []
                (let [{:keys [amplitude tumble show-bent]} @st
                      t (scene/tick! st)
                      rs (scene/update! ctx group {:t t :amp amplitude :tumble tumble
                                                   :show-bent show-bent})]
                  (scene/set-num! prefix "count" (scene/count-lit rs))
                  ;; zero by construction: geometry.cljs declares no donors, so
                  ;; there is no :molecule-donates pair to evaluate
                  (scene/set-num! prefix "gives"
                                  (count (filter #(and (:lit %) (= (:kind %) :molecule-donates)) rs)))
                  (scene/set-num! prefix "takes" (scene/count-lit rs :carbonyl))
                  (scene/set-num! prefix "methyl" (scene/count-lit rs :methyl))
                  (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

        (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame)))))
