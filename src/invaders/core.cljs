(ns invaders.core
  (:require [invaders.keyboard :refer [init-keyboard]]
            [invaders.game :refer [tick reset-entities!]]))

(enable-console-print!)

(init-keyboard)
(reset-entities!)
(tick)

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
