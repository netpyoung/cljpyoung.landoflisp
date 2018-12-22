(ns cljpyoung.landoflisp.common.rand
  (:refer-clojure :exclude [rand rand-int rand-nth]))

(def ^:dynamic *rand* nil)

(defn ->random [seed]
  (new java.util.Random seed))

(defn rand
  ([] (.nextFloat *rand*))
  ([n] (* n (rand))))

(defn rand-int
  [n]
  (int (rand n)))

(defn rand-nth
  [coll]
  (nth coll (rand-int (count coll))))
