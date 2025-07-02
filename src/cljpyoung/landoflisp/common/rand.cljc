(ns cljpyoung.landoflisp.common.rand
  (:refer-clojure :exclude [rand rand-int rand-nth]))

#?(:cljs
   (defn create-seeded-random [seed]
     (let [state (atom (mod seed 2147483647))]
       (when (<= @state 0)
         (reset! state (+ @state 2147483646)))
       (fn []
         (swap! state #(mod (* % 16807) 2147483647))
         (/ (dec @state) 2147483646.0))))
)

(def ^:dynamic *rand* nil)

(defn ->random [seed]
  #?(:clj (new java.util.Random seed)
     :cljs (create-seeded-random seed)))

(defn rand
  ([] #?(:clj (.nextFloat *rand*)
         :cljs (*rand*)))
  ([n] (* n (rand))))

(defn rand-int
  [n]
  (int (rand n)))

(defn rand-nth
  [coll]
  (nth coll (rand-int (count coll))))
