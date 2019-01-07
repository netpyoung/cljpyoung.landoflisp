(ns cljpyoung.landoflisp.common.rand
  (:refer-clojure :exclude [rand rand-int rand-nth])
  #?(:cljs (:import [goog.testing PseudoRandom])))


(def ^:dynamic *rand* nil)

(defn ->random [seed]
  #?(:clj (new java.util.Random seed)
     :cljs (new PseudoRandom seed)))

(defn rand
  ([] #?(:clj (.nextFloat *rand*)
         :cljs (.random *rand*)))
  ([n] (* n (rand))))

(defn rand-int
  [n]
  (int (rand n)))

(defn rand-nth
  [coll]
  (nth coll (rand-int (count coll))))
