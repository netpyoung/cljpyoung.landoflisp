(ns cljpyoung.landoflisp
  (:refer-clojure :exclude [rand])
  (:require
   [cljpyoung.landoflisp.ch19.core :as ch19]
   [cljpyoung.svg :as svg :include-macros true]
   [reagent.core :as r]
   [re-frame.core :as rf]
   ))



(defonce click-count (r/atom 0))



(declare main)

(defn on-js-reload []
  (enable-console-print!)
  (main)
  (println "on-js-reload"))

(defn app-root []
  [:div
   [ch19/test-page]
   [:button
    {:on-click #(rf/dispatch [:test 1])}]
   [:h2 "hello" @click-count]
   [:h2 "hello" @(rf/subscribe [:get-test])]
   ])

(rf/reg-event-db
 :init
 (fn [_ _]
   {:test 0}))

(rf/reg-event-db
 :test
 (fn [db [_ test-val]]
   (update-in db [:test] + test-val)))

(rf/reg-sub
 :get-test
 (fn [db _]
   (:test db)))

(defn ^:export main []
  (rf/dispatch [:init 1])
  (let [node (-> js/document (.getElementById  "app"))]
    (if-not node
      (js/alert "Fail to find `app`")
      (r/render-component
       [app-root]
       node))))
