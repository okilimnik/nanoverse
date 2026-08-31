(ns nanoverse.deck)

;; Deck plumbing: the bits every slide needs from Babylon, plus the
;; navigation that swaps one slide for another.
;;
;; Deliberately knows nothing about any particular slide -- `mount!` is handed
;; a list of slide specs. That keeps this namespace free of a cycle back to
;; the slides that require it; nanoverse.main is where the two meet.

(defn el
  "Look up an element by this slide's prefixed id, e.g. (el \"oh-\" \"amp\")."
  [prefix id]
  (js/document.getElementById (str prefix id)))

(defn hex->rgb01
  "Babylon's Color3/Color4 want 0-1 floats, never hex strings or 0-255."
  [hex]
  (let [h (subs hex 1)]
    [(/ (js/parseInt (subs h 0 2) 16) 255)
     (/ (js/parseInt (subs h 2 4) 16) 255)
     (/ (js/parseInt (subs h 4 6) 16) 255)]))

(defn make-material [scene hex glow?]
  (let [[r g b] (hex->rgb01 hex)
        m (js/BABYLON.StandardMaterial. (str "mat-" hex) scene)
        c (js/BABYLON.Color3. r g b)]
    (set! (.-diffuseColor m) c)
    ;; GlowLayer picks up anything with a non-black emissiveColor, so only
    ;; meshes meant to glow ever get one -- no exclusion list needed.
    (when glow? (set! (.-emissiveColor m) c))
    m))

(defn make-sphere [scene nm diameter material pos]
  (let [s (js/BABYLON.MeshBuilder.CreateSphere nm #js {:diameter diameter :segments 20} scene)]
    (set! (.-position s) pos)
    (set! (.-material s) material)
    s))

(defn make-tube [scene nm a-pos b-pos radius material]
  (let [t (js/BABYLON.MeshBuilder.CreateTube nm
            #js {:path #js [a-pos b-pos] :radius radius :tessellation 8} scene)]
    (set! (.-material t) material)
    t))

(defn make-hbond-tube
  "A reusable, updatable tube. Babylon's documented cheap-update pattern needs
   the SAME opts object back on every update, so it is returned alongside the
   mesh rather than rebuilt per frame."
  [scene radius material]
  (let [opts #js {:path #js [(js/BABYLON.Vector3. 0 0 0) (js/BABYLON.Vector3. 0 0 0.001)]
                  :radius radius :tessellation 8 :updatable true}
        mesh (js/BABYLON.MeshBuilder.CreateTube "hbond" opts scene)]
    (when material (set! (.-material mesh) material))
    (set! (.-isVisible mesh) false)
    {:mesh mesh :opts opts}))

(def reduce-motion?
  (and js/window.matchMedia
       (.-matches (js/window.matchMedia "(prefers-reduced-motion: reduce)"))))

(defn slide-handle
  "Wrap a built scene so the deck can switch it on and off. Only the visible
   slide runs a render loop, and only its camera is attached to the pointer --
   otherwise two cameras fight over the same drag."
  [engine scene camera canvas frame]
  {:engine engine
   :scene scene
   :start (fn []
            ;; resize first: a slide built while hidden measured a zero-size
            ;; canvas, which leaves Babylon with a 0x0 viewport
            (.resize engine)
            (.attachControl camera canvas true)
            (.runRenderLoop engine (fn [] (frame) (.render scene))))
   :stop (fn []
           (.stopRenderLoop engine)
           (.detachControl camera))})

;; ---------------------------------------------------------------------
;; navigation
;; ---------------------------------------------------------------------

(defn mount!
  "Wire up a deck of slides. Each spec is
     {:section-id \"slide-hydroxyl\" :prefix \"oh-\" :label \"Hydroxyl\" :build (fn [prefix] handle)}
   Slides are built lazily, on first visit, so an unopened slide never
   creates a WebGL context."
  [slides]
  (let [built (atom {})
        current (atom nil)
        prev-btn (js/document.getElementById "deck-prev")
        next-btn (js/document.getElementById "deck-next")
        dots-el (js/document.getElementById "deck-dots")
        pos-el (js/document.getElementById "deck-position")
        n (count slides)
        section (fn [i] (js/document.getElementById (:section-id (nth slides i))))]

    ;; one dot per slide, clickable
    (dotimes [i n]
      (let [d (js/document.createElement "button")]
        (set! (.-className d) "dot")
        (set! (.-type d) "button")
        (.setAttribute d "aria-label" (str "Go to " (:label (nth slides i))))
        (.appendChild dots-el d)))

    (letfn [(activate! [i]
              (when-not (= i @current)
                (when-let [c @current]
                  ((:stop (get @built c)))
                  (set! (.-className (section c)) "slide"))
                (let [sec (section i)]
                  ;; visible before build/start, so the canvas has a real size
                  (set! (.-className sec) "slide active")
                  (when-not (get @built i)
                    (let [spec (nth slides i)]
                      (swap! built assoc i ((:build spec) (:prefix spec)))))
                  ((:start (get @built i)))
                  (reset! current i)
                  (set! (.-textContent pos-el) (str (inc i) " / " n))
                  (dotimes [j n]
                    (set! (.-className (aget (.-children dots-el) j))
                          (str "dot" (if (= j i) " on" ""))))
                  (.focus sec #js {:preventScroll true}))))
            (step! [d] (activate! (mod (+ @current d) n)))
            ;; Squint compiles `dotimes` to a single `let i = 0` hoisted OUT of
            ;; the loop, so a closure made inside one captures the shared
            ;; binding and sees its final value (n) once the loop ends. Binding
            ;; the index through a function parameter is what actually gives
            ;; each handler its own copy.
            (bind-dot! [i]
              (.addEventListener (aget (.-children dots-el) i) "click"
                                 (fn [] (activate! i))))]

      (.addEventListener prev-btn "click" (fn [] (step! -1)))
      (.addEventListener next-btn "click" (fn [] (step! 1)))

      (dotimes [i n] (bind-dot! i))

      ;; Arrow keys drive the deck, but not while the user is working a
      ;; slider -- there the same keys are meant to nudge the value.
      (js/window.addEventListener "keydown"
        (fn [e]
          (let [tag (.-tagName (.-target e))]
            (when-not (or (= tag "INPUT") (= tag "TEXTAREA"))
              (cond
                (= (.-key e) "ArrowLeft") (do (.preventDefault e) (step! -1))
                (= (.-key e) "ArrowRight") (do (.preventDefault e) (step! 1)))))))

      (js/window.addEventListener "resize"
        (fn [] (when-let [c @current] (.resize (:engine (get @built c))))))

      (activate! 0))))
