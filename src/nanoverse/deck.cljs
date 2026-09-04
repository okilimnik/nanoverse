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

(defn- el+ [parent tag class text]
  (let [e (js/document.createElement tag)]
    (set! (.-className e) class)
    (when text (set! (.-textContent e) text))
    (.appendChild parent e)
    e))

(defn mount!
  "Wire up a deck of slides grouped into chapters. Each chapter is

     {:title \"Functional groups\" :blurb \"...\" :slides [spec ...]}

   and each slide spec is

     {:section-id \"slide-hydroxyl\" :prefix \"oh-\" :label \"Hydroxyl in Water\"
      :nav \"Hydroxyl\" :build (fn [prefix] handle)}

   Chapters exist because the deck outgrew a single row of anonymous dots. The
   side menu is the map of the whole thing and is always the same; the dots
   shrink to just the CURRENT chapter, so they stay a position indicator
   rather than becoming a second, worse menu.

   Slides are still built lazily, on first visit, so an unopened slide never
   creates a WebGL context -- which matters more now that there are twelve of
   them."
  [chapters]
  (let [slides (vec (mapcat (fn [[ci ch]]
                              (map (fn [s] (assoc s :chapter ci)) (:slides ch)))
                            (map-indexed vector chapters)))
        n (count slides)
        built (atom {})
        current (atom nil)
        nav-items (atom [])
        prev-btn (js/document.getElementById "deck-prev")
        next-btn (js/document.getElementById "deck-next")
        menu-btn (js/document.getElementById "deck-menu")
        nav-el (js/document.getElementById "deck-nav")
        scrim (js/document.getElementById "deck-scrim")
        dots-el (js/document.getElementById "deck-dots")
        pos-el (js/document.getElementById "deck-position")
        chapter-el (js/document.getElementById "deck-chapter")
        section (fn [i] (js/document.getElementById (:section-id (nth slides i))))
        ;; global index of the first slide in each chapter, so a slide can be
        ;; numbered within its own chapter rather than within the deck
        starts (reduce (fn [acc ch] (conj acc (+ (last acc) (count (:slides ch)))))
                       [0] chapters)]

    ;; one dot per slide, clickable; only the current chapter's are shown
    (dotimes [i n]
      (let [d (js/document.createElement "button")]
        (set! (.-className d) "dot")
        (set! (.-type d) "button")
        (.setAttribute d "aria-label" (str "Go to " (:label (nth slides i))))
        (.appendChild dots-el d)))

    (letfn [(close-menu! []
              (set! (.-className nav-el) "deck-nav")
              (when scrim (set! (.-hidden scrim) true))
              (when menu-btn (.setAttribute menu-btn "aria-expanded" "false")))
            (toggle-menu! []
              (if (= (.-className nav-el) "deck-nav open")
                (close-menu!)
                (do (set! (.-className nav-el) "deck-nav open")
                    (when scrim (set! (.-hidden scrim) false))
                    (when menu-btn (.setAttribute menu-btn "aria-expanded" "true")))))

            (activate! [i]
              (when-not (= i @current)
                (when-let [c @current]
                  ((:stop (get @built c)))
                  (set! (.-className (section c)) "slide"))
                (let [sec (section i)
                      spec (nth slides i)
                      ci (:chapter spec)
                      start (nth starts ci)
                      len (count (:slides (nth chapters ci)))]
                  ;; visible before build/start, so the canvas has a real size
                  (set! (.-className sec) "slide active")
                  (when-not (get @built i)
                    (swap! built assoc i ((:build spec) (:prefix spec))))
                  ((:start (get @built i)))
                  (reset! current i)
                  (set! (.-textContent pos-el) (str (inc (- i start)) " / " len))
                  (when chapter-el
                    (set! (.-textContent chapter-el) (:title (nth chapters ci))))
                  (dotimes [j n]
                    (set! (.-className (aget (.-children dots-el) j))
                          (str "dot"
                               (if (= (:chapter (nth slides j)) ci) "" " off")
                               (if (= j i) " on" ""))))
                  (dotimes [j (count @nav-items)]
                    (set! (.-className (nth @nav-items j))
                          (str "nav-item" (if (= j i) " on" ""))))
                  (close-menu!)
                  (.focus sec #js {:preventScroll true}))))

            (step! [d] (activate! (mod (+ @current d) n)))

            ;; Squint compiles `dotimes` to a single `let i = 0` hoisted OUT of
            ;; the loop, so a closure made inside one captures the shared
            ;; binding and sees its final value (n) once the loop ends. Binding
            ;; the index through a function parameter is what actually gives
            ;; each handler its own copy.
            (bind-dot! [i]
              (.addEventListener (aget (.-children dots-el) i) "click"
                                 (fn [] (activate! i))))
            (nav-item! [i spec group]
              (let [b (el+ group "button" "nav-item" nil)]
                (set! (.-type b) "button")
                (el+ b "span" "nav-num" (str (inc i)))
                (el+ b "span" "nav-text" (or (:nav spec) (:label spec)))
                (.addEventListener b "click" (fn [] (activate! i)))
                b))]

      ;; the side menu: the whole deck, laid out by chapter
      (when nav-el
        (el+ nav-el "p" "nav-brand" "nanoverse")
        (reset! nav-items
                (vec (mapcat (fn [[ci ch]]
                               (let [sec (el+ nav-el "div" "nav-chapter" nil)
                                     start (nth starts ci)]
                                 (el+ sec "h2" "nav-title" (:title ch))
                                 (when (:blurb ch) (el+ sec "p" "nav-blurb" (:blurb ch)))
                                 (mapv (fn [[k spec]] (nav-item! (+ start k) spec sec))
                                       (map-indexed vector (:slides ch)))))
                             (map-indexed vector chapters)))))

      (.addEventListener prev-btn "click" (fn [] (step! -1)))
      (.addEventListener next-btn "click" (fn [] (step! 1)))
      (when menu-btn (.addEventListener menu-btn "click" (fn [] (toggle-menu!))))
      (when scrim (.addEventListener scrim "click" (fn [] (close-menu!))))

      (dotimes [i n] (bind-dot! i))

      ;; Arrow keys drive the deck, but not while the user is working a
      ;; slider -- there the same keys are meant to nudge the value.
      (js/window.addEventListener "keydown"
        (fn [e]
          (let [tag (.-tagName (.-target e))]
            (cond
              (= (.-key e) "Escape") (close-menu!)
              (or (= tag "INPUT") (= tag "TEXTAREA")) nil
              (= (.-key e) "ArrowLeft") (do (.preventDefault e) (step! -1))
              (= (.-key e) "ArrowRight") (do (.preventDefault e) (step! 1))))))

      (js/window.addEventListener "resize"
        (fn [] (when-let [c @current] (.resize (:engine (get @built c))))))

      (activate! 0))))
