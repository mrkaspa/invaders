(ns invaders.draw)

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
