(ns invaders.game
  (:require [invaders.keyboard :refer [key-down? left right space]]
            [invaders.draw :refer [canvas-size screen draw]]
            [clojure.set :as cset]))

;; --- Entities ---
(defonce entities (atom []))

(defonce game-state (atom {:over false}))

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
              (< (:x %) (+ (:x invader) (/ (:width invader) 2)))
              (> (:x %) (- (:x invader) (/ (:width invader) 2)))
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
  (some #(if (colliding? entity %) [entity %]) entities))

(defn in-screen?
  [entity]
  (or (= :player (:type entity))
      (and
       (> (:x entity) 0)
       (> (:y entity) 0)
       (< (:x entity) canvas-size)
       (< (:y entity) canvas-size))))

;; --- Game Loop ---

(defn is-game-over?
  [entity-a entity-b]
  (or (= :player (:type entity-a)) (= :player (:type entity-b))))

(defn update-entities
  [entities]
  (let [old-entities entities
        col-entities-pair (keep
                           #(let [coll-res (colliding-with-anything? % entities)]
                              (if (and (in-screen? %)
                                       (some? coll-res)) coll-res)) entities)
        col-entities (map first col-entities-pair)
        entities (cset/difference (set old-entities) (set col-entities))
        new-entities (atom [])]
    (do
      ; (println col-entities)
      ; (if (not-empty col-entities)
      ;   (println col-entities " --- " (map (fn [elem] (println elem "--->")) col-entities)))
      (if (and 
           (not-empty col-entities-pair) 
           (some #(do (println "--->" ) (is-game-over? (first %) (second %))) col-entities-pair))
        ; (swap! game-state assoc :over false)
        (println "GAME OVER"))
      (with-redefs [add-entity! #(swap! new-entities conj %)]
        (into (mapv update-entity entities)
              @new-entities)))))

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
                  (+ 30 (* y 30)))))

(defn reset-entities!
  []
  (reset! entities (conj invaders player)))
