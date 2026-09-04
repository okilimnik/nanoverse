(ns nanoverse.aminoacid.babylon-core
  (:require [nanoverse.deck :as deck]
            [nanoverse.scene :as scene]
            [nanoverse.solvent :as sol]
            [nanoverse.aminoacid.geometry :as geo]))

;; Babylon rendering for the four amino-acid slides.
;;
;; ONE builder, parameterised by side-chain class, rather than four
;; near-identical files. That is not just to save typing: the four slides are
;; meant to be one experiment run four times, and if the code that draws them
;; is literally the same code, then nothing about the backbone, the water
;; model, the bond test or the readouts can differ between them by accident.
;; The only thing a slide chooses is which residues appear in its picker.
;;
;; Every residue's states are built up front and switching is a visibility
;; flip, exactly as the pH slides do it -- clicking through twenty-one
;; residues never rebuilds a mesh.

(defn- count-kind [rs kind]
  (count (filter #(and (:lit %) (= (:kind %) kind)) rs)))

(defn- chip!
  "One residue button. The index is passed as a FUNCTION PARAMETER on purpose:
   Squint compiles `dotimes` to a single hoisted binding, so a handler created
   inside the loop body would close over the loop's final value and every chip
   would select the last residue."
  [container r i on-pick]
  (let [b (js/document.createElement "button")
        one (js/document.createElement "span")
        three (js/document.createElement "span")]
    (set! (.-type b) "button")
    (set! (.-className b) (str "chip" (case (:sign r) :positive " pos" :negative " neg" "")))
    (.setAttribute b "aria-label" (:name r))
    (set! (.-className one) "chip-one")
    (set! (.-textContent one) (:one r))
    (set! (.-className three) "chip-three")
    (set! (.-textContent three) (:three r))
    (.appendChild b one)
    (.appendChild b three)
    (.addEventListener b "click" (fn [] (on-pick i)))
    (.appendChild container b)
    b))

(defn builder
  "Returns the `build` function for one side-chain class."
  [class-key]
  (fn [prefix]
    (let [residues (geo/build class-key)
          ;; Frame the slide from its own contents rather than a guessed
          ;; radius: tryptophan with its water shell is more than twice the
          ;; size of glycine, and one hard-coded distance either clips the big
          ;; residues or strands the small ones in the middle of an empty box.
          view (geo/view residues)
          ;; Babylon's default vertical field of view is 0.8 rad, so a sphere
          ;; of radius r fits at about r/tan(0.4); the constant adds room for
          ;; the wobble and for a water's own hydrogens.
          dist (+ 0.9 (* 2.45 scene/UNITS-PER-A (:extent view)))
          ctx (scene/init prefix {:alpha (- (/ js/Math.PI 2) 0.75) :beta 1.2
                                  :radius dist :lower (* 0.55 dist) :upper (* 2.4 dist)})
          ;; groups is indexed [residue][protonation form]
          groups (mapv (fn [r] (mapv #(scene/state-group! ctx %) (:states r))) residues)
          st (scene/controls! prefix)
          ui (atom {:res 0 :ph geo/CELL-PH})
          current (atom nil)
          picker (deck/el prefix "residues")
          chips (atom [])
          ph-input (deck/el prefix "ph")
          ph-out (deck/el prefix "ph-value")
          phobic-toggle (deck/el prefix "phobic")]

      ;; every residue is posed in the same backbone frame, so ONE camera
      ;; target serves the whole slide and clicking a chip never pans
      (set! (.-target (:camera ctx)) (scene/to-vec3 (:center view)))

      ;; nothing on screen until something is picked
      (doseq [gs groups] (doseq [g gs] (scene/show! g false)))

      (letfn [(paint-phobic! [on?]
                (doseq [[i r] (map-indexed vector residues)]
                  (doseq [g (nth groups i)]
                    (doseq [k (:greasy r)]
                      (when-let [m (get (:atom-meshes (:mol g)) k)]
                        (set! (.-material m)
                              (if on?
                                (:phobic (:materials ctx))
                                (get (:materials ctx) (:e (get (:atoms (:state g)) k))))))))))

              (readouts! [r j g]
                (let [state (:state g)
                      ph (:ph @ui)
                      frac (geo/fraction-charged r ph)]
                  (scene/set-text! prefix "name" (str (:name r) " · " (:three r) " · " (:one r)))
                  (scene/set-text! prefix "blurb" (:blurb r))
                  (scene/set-text! prefix "species" (:label state))
                  (scene/set-text! prefix "charge" (sol/signed-charge (:charge state)))
                  (scene/set-text! prefix "pka" (if (:pka r) (.toFixed (:pka r) 2) "none in water"))
                  (scene/set-text! prefix "hydropathy"
                                   (if (:hydropathy r) (.toFixed (:hydropathy r) 1) "not tabulated"))
                  (scene/set-text! prefix "charged"
                                   (if frac (str (.toFixed (* 100 frac) 1) "%") "never"))
                  ;; how many hydrogens the backbone nitrogen actually has --
                  ;; the readout proline exists to move
                  (scene/set-text! prefix "backbone-h"
                                   (str (:backbone-h r) " on N"
                                        (if (= (:backbone-h r) 2) " (2, not 3)" "")))
                  (scene/set-text! prefix "phobic-waters" (:phobic-waters r))
                  (scene/set-text! prefix "forms"
                                   (if (> (count (:states r)) 1)
                                     (str "form " (inc j) " of " (count (:states r)))
                                     "one form only"))))

              (apply! []
                (let [{:keys [res ph]} @ui
                      r (nth residues res)
                      j (geo/form-index r ph)
                      g (nth (nth groups res) j)]
                  (when-not (= g @current)
                    (when @current (scene/show! @current false))
                    (scene/show! g true)
                    (reset! current g))
                  (dotimes [i (count @chips)]
                    (set! (.-className (nth @chips i))
                          (str "chip"
                               (case (:sign (nth residues i)) :positive " pos" :negative " neg" "")
                               (if (= i res) " on" ""))))
                  (readouts! r j g)))

              (pick! [i]
                (swap! ui assoc :res i)
                (apply!))

              (set-ph! [ph]
                (swap! ui assoc :ph ph)
                (when ph-out (set! (.-textContent ph-out) (.toFixed ph 1)))
                (apply!))]

        (when picker
          (reset! chips (mapv (fn [[i r]] (chip! picker r i pick!))
                              (map-indexed vector residues))))

        (when phobic-toggle
          (paint-phobic! (.-checked phobic-toggle))
          (.addEventListener phobic-toggle "change"
            (fn [] (paint-phobic! (.-checked phobic-toggle)))))

        (if ph-input
          (do (set-ph! (js/parseFloat (.-value ph-input)))
              (.addEventListener ph-input "input"
                (fn [] (set-ph! (js/parseFloat (.-value ph-input))))))
          (apply!))

        (letfn [(frame []
                  (let [{:keys [amplitude tumble show-bent]} @st
                        t (scene/tick! st)
                        rs (scene/update! ctx @current {:t t :amp amplitude :tumble tumble
                                                        :show-bent show-bent})]
                    (scene/set-num! prefix "count" (scene/count-lit rs))
                    (scene/set-num! prefix "gives" (count-kind rs :molecule-donates))
                    (scene/set-num! prefix "takes" (count-kind rs :water-donates))
                    (scene/set-num! prefix "bent" (count (filter :bent rs)))))]

          (deck/slide-handle (:engine ctx) (:scene ctx) (:camera ctx) (:canvas ctx) frame))))))

;; The four slides. Same builder, different residues -- which is the argument
;; the chapter is making about the classification itself.
(def build-charged (builder :charged))
(def build-polar (builder :polar))
(def build-special (builder :special))
(def build-nonpolar (builder :nonpolar))
