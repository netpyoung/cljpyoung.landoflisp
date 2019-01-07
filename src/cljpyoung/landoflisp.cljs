(ns cljpyoung.landoflisp
  (:refer-clojure :exclude [rand])
  (:require
   [cljpyoung.landoflisp.ch19.core :as ch19]
   [cljpyoung.svg :as svg :include-macros true]
   [cljpyoung.landoflisp.common.rand :as rand]
   [reagent.core :as r]))



(defonce click-count (r/atom 0))



(declare main)

(defn on-js-reload []
  (enable-console-print!)
  (main)
  (println "on-js-reload"))


(defn hello [name]
  [:h3 "hello" name])

(defn x []
  [:div
   [hello "hi"]
   [:h2 "hello" @click-count]
   [:h2 "hello" click-count]
   ])

(defn contact-list []
  [:div
   (svg/svg 300 100
            (svg/polygon
             [[50 30] [74 20] [74 50] [50 60]]
             [115 0 0])
            (svg/polygon
             [[150 30] [74 20] [74 50] [50 60]]
             [215 0 0])
            )
   [x]
   [:h1 "23"]])

(defn ^:export main []
  (let [node (-> js/document (.getElementById  "app"))]
    (if-not node
      (js/alert "Fail to find `app`")
      (r/render-component
       [contact-list]
       node))))
