(ns cljpyoung.svg
  (:require
   [clojure.string :as str]

   ;; ref: https://clojurescript.org/reference/google-closure-library
   #?(:cljs [goog.string :as gstring])
   #?(:cljs [goog.string.format :as format])))


#?(:cljs
   (def format goog.string.format))

(defn str-points [points]
  (->> points
       (map #(str/join "," %))
       (str/join " ")))

(defn brightness [color amount]
  (mapv #(min 255 (max 0 (+ % amount))) color))

(defn str-rgb [color]
  (let [[r g b] color]
    (format "rgb(%s,%s,%s)" r g b)))

(defn style [color]
  {:fill (str-rgb color)
   :stroke (str-rgb (brightness color -100))})

(defn polygon [points color]
  ^{:key (str "polygon:" points color)}
  [:polygon {:points (str-points points)
             :style (style color)}])

(defmacro svg [w h & body]
  `[:svg {:style {:border "1px solid"
                  :background "white"
                  :width (str ~w)
                  :height (str ~h)}}
    ~@body])
