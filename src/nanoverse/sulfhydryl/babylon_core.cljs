(ns nanoverse.sulfhydryl.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.sulfhydryl.geometry :as geo]))

;; Babylon rendering for the sulfhydryl slide.
;;
;; This slide has two knobs of its own, and the first of them exists to
;; undermine the deck's own bond test.
;;
;; The geometric proxy every previous slide has used -- H...acceptor under
;; 2.5 A, D-H...A over 120 degrees -- cannot tell an O-H hydrogen bond from an
;; S-H one. Hand it a thiol at thiol distances and it lights the bond up just
;; as brightly. It is not wrong about the geometry; it is blind to the thing
;; that actually makes a hydrogen bond strong, which is how hard the donor
;; pulls electron density off its own hydrogen.
;;
;; So the "minimum strength" slider filters the same bonds by a TABULATED
;; typical enthalpy for their donor/acceptor element pair (solvent/STRENGTH) --
;; numbers looked up, not computed from anything on screen. Raise it and the
;; sulfur bonds die first while the oxygen ones stay lit. That is the lesson,
;; and the slider is also the confession.

(defn build [prefix]
  (let [ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.3) :beta 1.3 :radius 11.5
                                :lower 5 :upper 28})
        groups (mapv #(scene/state-group! ctx %) geo/states)
        st (scene/controls! prefix)
        active (atom (nth groups 0))
        oxidize (deck/el prefix "oxidize")
        strength-input (deck/el prefix "strength")
        strength-out (deck/el prefix "strength-value")]

    (scene/set-text! prefix "co" (str (.toFixed geo/CO-LENGTH 3) " Å"))
    (scene/set-text! prefix "cs" (str (.toFixed geo/CS-LENGTH 3) " Å"))

    (letfn [(set-redox! [ox?]
              (scene/show! (nth groups 0) (not ox?))
              (scene/show! (nth groups 1) ox?)
              (reset! active (nth groups (if ox? 1 0)))
              (scene/set-text! prefix "species" (:label (:state @active)))
              (scene/set-text! prefix "ss" (if ox? (str (.toFixed geo/SS-LENGTH 3) " Å") "—")))]

      (set-redox! false)
      (when oxidize
        (.addEventListener oxidize "change" (fn [] (set-redox! (.-checked oxidize)))))

      (when strength-input
        (swap! st assoc :min-strength (js/parseFloat (.-value strength-input)))
        (.addEventListener strength-input "input"
          (fn []
            (let [x (js/parseFloat (.-value strength-input))]
              (swap! st assoc :min-strength x)
              (when strength-out
                (set! (.-textContent strength-out) (str (.toFixed x 0) " kJ/mol")))))))

      (letfn [(frame []
                (let [{:keys [amplitude tumble show-bent min-strength]} @st
                      t (scene/tick! st)
                      rs (scene/update! ctx @active {:t t :amp amplitude :tumble tumble
                                                     :show-bent show-bent
                                                     :min-strength (or min-strength 0)})
                      ;; both sulfur sites, whichever redox state is showing
                      sulfur (+ (scene/count-lit rs :sulfhydryl) (scene/count-lit rs :disulfide))]
                  (scene/set-num! prefix "count" (scene/count-lit rs))
                  (scene/set-num! prefix "oh" (scene/count-lit rs :hydroxyl))
                  (scene/set-num! prefix "thiol" sulfur)
                  (scene/set-text! prefix "energy"
                    (str (reduce + 0 (map :strength (filter :lit rs))) " kJ/mol"))
                  (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

        (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame)))))
