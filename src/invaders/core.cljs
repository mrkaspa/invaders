(ns invaders.core
  (:require ))


(enable-console-print!)

;; -- Drawing ---
(def canvas-size 300)
(def canvas (. js/document  (getElementById "space-invaders")))
(def screen (. canvas (getContext "2d")))

(defn draw-rect
  "Draws a rect on a screen at coordinates x,y with width w and height h"
  [screen x y w h]
  (.fillRect screen x y w h))

(defn draw-circle
  [screen x y r]
  (doto screen
    (.beginPath)
    (.arc x y r 0 (* 2 Math/PI))
    (.closePath)
    (.fill)))

(defn draw
  "Draw an entity to the (global) screen"
  [{:keys [type x y width] :as entity}]
  (case type
    :player (draw-circle screen x y 7)
    :bullet (draw-circle screen x y 2)
    (draw-rect screen
               (- x (/ width 2))
               (- y (/ width 2))
               width width)))

;; --- Keyboard ---

(def keyboard (atom {}))

(defn init-keyboard
  "Initialize listeners for the keyboard"
  []
  (. js/window (addEventListener "keydown" #(swap! keyboard assoc (.-keyCode %) true)))
  (. js/window (addEventListener "keyup" #(swap! keyboard assoc (.-keyCode %) false))))

(defn key-down?
  [keycode]
  (get @keyboard keycode))

(def left 37)
(def right 39)
(def space 32)

;; --- Entities ---
(defonce entities (atom []))

(defn add-entity!
  [entity]
  (swap! entities conj entity))

(defmulti update-entity
  "Update a player's position on the screen"
  (fn [entity] (:type entity)))

(defmethod update-entity :default
  [entity]
  entity)

;; --- Bullet ---

(defn make-bullet
  [x y velocity-x velocity-y]
  {:type :bullet
   :x x
   :y y
   :velocity-x velocity-x
   :velocity-y velocity-y
   :width 3})

(defn shoot
  [entity velocity-x velocity-y]
  (let [x (:x entity)
        y (if (pos? velocity-y)
            (+ (:y entity)
               (:width entity))
            (- (:y entity)
               (:width entity)))]
    (add-entity! (make-bullet x y velocity-x velocity-y))
    entity))

(defmethod update-entity :bullet
  [bullet]
  (-> bullet
      (update :x #(+ % (:velocity-x bullet)))
      (update :y #(+ % (:velocity-y bullet)))))

;; --- Player ---

(def player
  {:type :player
   :x 150
   :y 285
   :width 15
   :speed-x 1})

(defmethod update-entity :player
  [{:keys [x speed-x] :as player}]
  (cond-> player
    (and (key-down? left) (> x 15))
    (update :x #(- % speed-x))

    (and (key-down? right) (< x 285))
    (update :x #(+ % speed-x))

    (key-down? space)
    (shoot 0 -7)))

;; --- Invader ---
(defn make-invader
  [x y]
  {:type :invader
   :x x
   :y y
   :width 15
   :patrol-x 0
   :velocity-x 0.4})

(defn maybe-turn-around
  "Turn the invader around if it's walked far enough"
  [invader]
  (if (= 0 (mod (:patrol-x invader) 100))
    (update invader :velocity-x -)
    invader))

(defn invaders-below?
  "True if there are invaders below this invader"
  [invader]
  (some #(and (= :invader (:type %))
              (< (:x %) (+ (:x invader) (/ (:width invader) 2) ))
              (> (:x %) (- (:x invader) (/ (:width invader) 2) ))
              (< (:y invader) (:y %)))
        @entities))

(defn maybe-shoot
  [invader]
  (when (and (not (invaders-below? invader))
             (> (rand) 0.995))
    (shoot invader (- (rand) 0.5) 2))
  invader)

(defmethod update-entity :invader
  [invader]
  (-> invader
      (update :x #(+ % (:velocity-x invader)))
      (update :patrol-x inc)
      maybe-turn-around
      maybe-shoot))

;; --- Collision Detection ---

(defn colliding?
  [a b]
  (not
   (or (identical? a b)

       (< (+ (:x a) (/ (:width a)) 2)
          (- (:x b) (/ (:width b)) 2))

       (< (+ (:y a) (/ (:width a)) 2)
          (- (:y b) (/ (:width b)) 2))

       (> (- (:x a) (/ (:width a)) 2)
          (+ (:x b) (/ (:width b)) 2))

       (> (- (:y a) (/ (:width a)) 2)
          (+ (:y b) (/ (:width b)) 2)))))

(defn colliding-with-anything?
  [entity entities]
  (some #(colliding? entity %) entities))

(defn in-screen?
  [entity]
  (or (= :player (:type entity))
      (and
       (> (:x entity) 0)
       (> (:y entity) 0)
       (< (:x entity) canvas-size)
       (< (:y entity) canvas-size))))

;; --- Game Loop ---

(defn update-entities
  [entities]
  (let [entities (filter #(and (in-screen? %)
                               (not (colliding-with-anything? % entities))) entities)
        new-entities (atom [])]
    (with-redefs [add-entity! #(swap! new-entities conj %)]
      (into (mapv update-entity entities)
            @new-entities))))

(defn tick
  []
  (.clearRect screen 0 0 canvas-size canvas-size)
  (doall (map draw @entities))
  (swap! entities update-entities)
  (. js/window (requestAnimationFrame tick)))

;; --- Initial state ---
(def invaders
  (for [x (range 8)
        y (range 3)]
    (make-invader (+ 30 (* x 30))
                  (+ 30 (* y 30 )))))

(defn reset-entities!
  []
  (reset! entities (conj invaders player)))

(init-keyboard)
(reset-entities!)
(tick)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )

