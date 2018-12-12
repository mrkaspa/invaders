(ns invaders.keyboard)

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
