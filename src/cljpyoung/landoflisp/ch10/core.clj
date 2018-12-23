(ns cljpyoung.landoflisp.ch10.core
  (:require [cljpyoung.landoflisp.common.rand :as rand]))

;; evolve

;; const
(def WIDTH 100)
(def HEIGHT 30)
(def REPRODUCTION_ENERGY 200)
(def JUNGLE [45 10 10 10])
(def PLANT_ENERGY 80)

;; variables
(def &random (atom (rand/->random 100)))
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))

(defrecord Animal [x y energy dir genes])
(def &plant-poses (atom #{}))
(def &animals (atom []))

;; functions
(defn random-plant [left top width height]
  [(+ left (random width)) (+ top (random height))])

(defn add-plants [poses]
  (-> poses
      (conj (apply random-plant JUNGLE))
      (conj (random-plant 0 0 WIDTH HEIGHT))))

(defn move [{:keys [dir x y] :as animal}]
  (-> animal
      (assoc :x (-> (cond (and (>= dir 2) (< dir 5)) 1
                          (or  (=  dir 1) (= dir 5)) 0
                          :else -1)
                    (+ x WIDTH)
                    (mod WIDTH)))
      (assoc :y (-> (cond (and (>= dir 0) (< dir 3)) -1
                          (and (>= dir 4) (< dir 7)) 1
                          :else 0)
                    (+ y HEIGHT)
                    (mod HEIGHT)))
      (update :energy dec)))

(defn turn [{:keys [genes dir] :as animal}]
  (letfn [(angle [genes x]
            (let [[fst & rst] genes, xnu (- x fst)]
              (if (neg? xnu)
                0
                (inc (angle rst xnu)))))]
    (assoc animal :dir (-> genes
                           (angle (random (apply + genes)))
                           (+ dir)
                           (mod 8)))))

(defn eat [{:keys [x y] :as animal} &poses]
  (let [pos [x y]]
    (if-not (contains? @&poses pos)
      animal
      (do
        (swap! &poses disj pos)
        (update animal :energy + PLANT_ENERGY)))))

(defn reproduce [{:keys [energy genes] :as animal}]
  (if (< energy REPRODUCTION_ENERGY)
    [animal]
    (let [new-energy (quot energy 2)
          mutation  (random 8)
          new-genes (->> (max 1 (+ (nth genes mutation) (random 3) -1))
                         (assoc genes mutation))]
      [(assoc animal :energy new-energy)
       (assoc animal :energy new-energy :genes new-genes)])))

(defn update-world []
  (->> @&animals
       (remove #(not (pos? (:energy %))))
       (mapcat #(-> % (turn) (move) (eat &plant-poses) (reproduce)))
       (vec)
       (reset! &animals))
  (swap! &plant-poses add-plants))

(defn draw-world []
  (let [animals @&animals
        plant-poses @&plant-poses]
    (doseq [y (range HEIGHT)]
      (print "|")
      (doseq [x (range WIDTH)]
        (print (cond (some #(= [x y] [(:x %) (:y %)]) animals) \M
                     (contains? plant-poses [x y]) \*
                     :else \space)))
      (println "|"))))

(defn evolution []
  (reset! &random (rand/->random 100))
  (reset! &plant-poses #{})
  (reset! &animals [(map->Animal
                     {:x      (quot WIDTH  2)
                      :y      (quot HEIGHT 2)
                      :energy 1000
                      :dir    0
                      :genes  (vec (repeatedly 8 #(inc (random 10))))})])
  (loop []
    (draw-world)
    (let [s (read-line)]
      (when-not (= "quit" s)
        (if-let [x (try (Integer/parseInt s) (catch Exception _))]
	  (doseq [i (range x)]
	    (update-world)
	    (when (zero? (rem i 1000))
	      (println \.)))
	  (update-world))
        (recur)))))
