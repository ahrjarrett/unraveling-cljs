(ns unravelingcljs.core
  (:require  [goog.dom :as dom]
             [goog.events :as events]
             [unravelingcljs.basics :as basics]
             [unravelingcljs.advanced :as advanced]))

(enable-console-print!)

(println "core loaded")

(def app-state (atom {:text "Hello world!"}))
(reset! app-state
        {:text (->>
                basics/evolved-master
                :main-man)
         :price 0})

(defn on-js-reload [])

(defn pr-state []
  (do (println "App state:\n" @app-state)))

(defn unfreeify [event]
  (let [msg "message"]
    (swap! app-state update :price inc)
    (pr-state)
    (dom/setTextContent (dom/getElement "pricetag")
                        (str "only $" (@app-state :price) ".00!"))))


(events/listen (dom/getElement "app") "click" unfreeify)
