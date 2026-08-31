(ns nanoverse.vec3)

;; Minimal 3-vector and quaternion math shared by every visualization's
;; geometry namespace. Deliberately has no rendering-engine dependency, so
;; the chemistry code that builds on it stays renderer-independent.
;;
;; Vectors are plain maps {:x :y :z}; quaternions are {:x :y :z :w}.

(defn add [a b] {:x (+ (:x a) (:x b)) :y (+ (:y a) (:y b)) :z (+ (:z a) (:z b))})
(defn sub [a b] {:x (- (:x a) (:x b)) :y (- (:y a) (:y b)) :z (- (:z a) (:z b))})
(defn scale [a s] {:x (* (:x a) s) :y (* (:y a) s) :z (* (:z a) s)})

(defn dot [a b] (+ (* (:x a) (:x b)) (* (:y a) (:y b)) (* (:z a) (:z b))))

(defn cross [a b]
  {:x (- (* (:y a) (:z b)) (* (:z a) (:y b)))
   :y (- (* (:z a) (:x b)) (* (:x a) (:z b)))
   :z (- (* (:x a) (:y b)) (* (:y a) (:x b)))})

(defn len [a] (js/Math.sqrt (dot a a)))

(defn norm [a]
  (let [l (len a)]
    (if (zero? l) {:x 0 :y 0 :z 0} (scale a (/ 1 l)))))

(defn centroid [vs]
  (scale (reduce add {:x 0 :y 0 :z 0} vs) (/ 1 (count vs))))

;; --- quaternions -------------------------------------------------------
;; Used for the per-water tumble. Kept as explicit q*v*q^-1 rather than a
;; matrix so the geometry namespace and the renderer can be handed the exact
;; same four numbers and cannot disagree about Euler-angle ordering.

(defn quat-axis-angle [axis theta]
  (let [u (norm axis)
        h (/ theta 2)
        s (js/Math.sin h)]
    {:x (* (:x u) s) :y (* (:y u) s) :z (* (:z u) s) :w (js/Math.cos h)}))

(defn rotate
  "Rotate vector v by unit quaternion q."
  [q v]
  (let [qv {:x (:x q) :y (:y q) :z (:z q)}
        t (scale (cross qv v) 2)]
    (add (add v (scale t (:w q))) (cross qv t))))

;; --- angles ------------------------------------------------------------

(defn angle-deg
  "Angle at vertex b, in degrees, for the path a-b-c."
  [a b c]
  (let [u (norm (sub a b))
        v (norm (sub c b))
        c* (js/Math.max -1 (js/Math.min 1 (dot u v)))]
    (/ (* (js/Math.acos c*) 180) js/Math.PI)))
