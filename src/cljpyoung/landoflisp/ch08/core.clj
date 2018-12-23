(ns cljpyoung.landoflisp.ch08.core
  (:require [cljpyoung.landoflisp.common.graph-util :as graph-util])
  (:require [clojure.set :as set])
  (:require [cljpyoung.landoflisp.common.rand :as rand]))

;; The Grand Theft Wumpus Game.

;; const
(def NUM_NODE 30)
(def NUM_EDGE 45)
(def NUM_WORM 3)
(def NUM_COP 15)

;; variable
(def &player-pos (atom -1))
(def &congestion-city-nodes (atom nil))
(def &congestion-city-edges (atom nil))
(def &visited-nodes (atom #{}))
(def &random (atom (rand/->random 100)))

;; functions
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))

(defn random-node []
  (inc (random NUM_NODE)))

;; node : number
;; edge : [node node]
;; edge-alist : {from-node {to-node [info...] ...} ...}

(defn edge-pair [a b]
  (if (= a b)
    []
    [[a b] [b a]]))

(defn make-edge-list
  []
  (->> (repeatedly NUM_EDGE #(edge-pair (random-node) (random-node)))
       (remove empty?)
       (apply concat)
       (set)
       (vec)))

(defn direct-edges
  "
  ;; (direct-edges 2 [[1 2] [2 1]])
  ;;=> ([2 1])"
  [node edge-list]
  (filter (fn [[from _]] (= from node)) edge-list))

(defn get-connected
  "
  ;; (get-connected 1 [[1 2] [2 3] [3 4]])
  ;;=> #{1 4 3 2}"
  [node edge-list]
  (loop [connected #{}, [fst & rst] [node]]
    (if-not fst
      connected
      (->> edge-list
           (direct-edges fst)
           (map (fn [[_ to]] to))
           (remove connected)
           (into rst)
           (recur (conj connected fst))))))


(defn find-islands
  "
  ;; (find-islands [3 1] [[3 2] [1 1] [2 3]])
  ;;=> #{#{1} #{3 2}}"
  [nodes edge-list]
  (loop [islands #{}
         [fst & rst] (sort nodes)]
    (if-not fst
      islands
      (let [connected (get-connected fst edge-list)]
        (recur (conj islands connected)
               (set/difference (set rst) connected))))))

(defn connect-with-bridges
  "
  ;;(connect-with-bridges [#{1 2} #{3 4}])
  ;;=> ([1 4] [4 1])"
  [islands]
  (loop [acc []
         [fst & rst] islands]
    (cond (nil? fst) acc
          (nil? rst) acc
          :else
          (recur (concat acc (edge-pair (first fst) (ffirst rst)))
                 rst))))

(defn connect-all-islands
  "
  ;;(connect-all-islands [1 2 3 4 5] [[1 2] [2 3] [4 5] [5 4]])
  ;;=> ([1 4] [4 1] [1 2] [2 3] [4 5] [5 4])"
  [nodes edge-list]
  (-> (find-islands nodes edge-list)
      (connect-with-bridges)
      (concat edge-list)))

(declare edges-to-alist)
(declare add-cops)

(defn make-city-edges
  " (make-city-edges)
  ;; => {from-node {to-node [...] ...} ...}"
  []
  (let [nodes (range 1 NUM_NODE)
        edge-list (make-edge-list)
        edge-list (connect-all-islands nodes edge-list)
        cops (reduce (fn [acc edge]
                       (if (zero? (random NUM_COP))
                         (conj acc edge)
                         acc))
                     edge-list)]
    (-> edge-list
        (edges-to-alist)
        (add-cops cops))))

(defn edges-to-alist
  "
  ;; (edges-to-alist [[1 2] [1 3]])
  ;;=> {1 {2 [], 3 []}}"
  [edge-list]
  (->> edge-list
       (map (fn [[from _]] from))
       (reduce conj #{})
       (reduce (fn [acc node]
                 (->> (direct-edges node edge-list)
                      (map (fn [[_ to]] [to []]))
                      (reduce conj {})
                      (assoc acc node)))
               {})))

(defn add-cops
  "
  ;; (add-cops {1 {3 [] 2 []} 2 {4 [] 5 []}} [[1 3]])
  ;;=> {1 {3 [:cops], 2 []}, 2 {4 [], 5 []}}"
  [edge-alist edges-with-cops]
  (let [edges-with-cops (set edges-with-cops)]
    (->> edge-alist
         (map (fn [[node dests]]
                (->> dests
                     (mapv (fn [[k v]]
                             (if-not (empty? (set/intersection (set (edge-pair node k)) edges-with-cops))
                               [k [:cops]]
                               [k []])))
                     (into {})
                     (vector node))))
         (into {}))))

(defn neighbors
  "
  ;;(neighbors 1 {1 {3 [] 2 []}})
  ;;=> (3 2)"
  [node edge-alist]
  (keys (get edge-alist node)))

(defn within-one
  "
  ;; (within-one 1 2 {1 {3 [] 2 []}})
  ;;=> true"
  [a b edge-alist]
  (->> (neighbors a edge-alist)
       (some #{b})
       (some?)))

(defn within-two
  "
  ;; (within-two 1 4 {1 {3 [] 2 []} 2 {4 []}})
  ;;=> true"
  [a b edge-alist]
  (or (within-one a b edge-alist)
      (->> (neighbors a edge-alist)
           (some #(within-one % b edge-alist))
           (some?))))

(defn make-city-nodes
  "
  ;; (make-city-nodes (make-city-edges))
  ;;=> {node [signal ...] ...}"
  [edge-alist]
  (let [wumpus (random-node)
        glow-worms (repeatedly NUM_WORM random-node)]
    (->> (range 1 (inc NUM_NODE))
         (mapv (fn [node]
                 (->> [(cond (= node wumpus) :wumpus
                             (within-two node wumpus edge-alist) :blood!)
                       (cond (some #{node} glow-worms) :glow-worm
                             (some #(within-one node % edge-alist) glow-worms) :lights!)
                       (when (some (complement empty?) (vals (get edge-alist node)))
                         :sirens!)]
                      (filter some?)
                      (vec)
                      (vector node))))
         (into {}))))

(declare find-empty-node)
(declare draw-city)

(defn find-empty-node []
  (let [node (random-node)]
    (if (empty? (get @&congestion-city-nodes node))
      node
      (recur))))

(defn draw-city []
  (graph-util/ugraph->png "city" @&congestion-city-nodes @&congestion-city-edges))

(defn known-city-nodes []
  (let [visited-nodes @&visited-nodes]
    (->> visited-nodes
         (mapcat #(neighbors % @&congestion-city-edges))
         (concat visited-nodes)
         (distinct)
         (mapv (fn [node]
                 (if (contains? visited-nodes node)
                   (let [n (get @&congestion-city-nodes node)]
                     (if (= node @&player-pos)
                       [node (conj n :*)]
                       [node n]))
                   [node [:?]]))))))

(defn known-city-edges []
  (let [visited-nodes @&visited-nodes
        congestion-city-edges @&congestion-city-edges]
    (->> visited-nodes
         (map (fn [node]
                (->> node
                     (get congestion-city-edges)
                     (mapv (fn [x]
                             (let [[to _] x]
                               (if (get visited-nodes to)
                                 x
                                 [to []]))))
                     (into {})
                     (vector node))))
         (into {}))))

(defn draw-known-city []
  (graph-util/ugraph->png "known-city" (known-city-nodes) (known-city-edges)))

(defn new-game
  ([] (new-game (rand-int 9999999)))
  ([random-seed]
   (reset! &random (rand/->random random-seed))
   (reset! &congestion-city-edges (make-city-edges))
   (reset! &congestion-city-nodes (make-city-nodes @&congestion-city-edges))
   (reset! &player-pos (find-empty-node))
   (reset! &visited-nodes #{@&player-pos})
   (draw-city)
   (draw-known-city)))


(declare handle-direction)
(declare handle-new-place)
(defn walk [pos]
  (handle-direction pos false))

(defn charge [pos]
  (handle-direction pos true))

(defn handle-direction [pos is-charging]
  (let [player-pos @&player-pos
        congestion-city-edges @&congestion-city-edges]
    (let [edge (get (get congestion-city-edges player-pos) pos)]
      (if edge
        (handle-new-place edge pos is-charging)
        (print "That location does not exist!")))))

(defn handle-new-place [edge pos is-charging]
  (let [congestion-city-nodes @&congestion-city-nodes
        node (set (get congestion-city-nodes pos))
        has-worm (and (contains? node :glow-worm)
                      (not (contains? @&visited-nodes pos)))]
    (swap! &visited-nodes conj pos)
    (reset! &player-pos pos)
    (draw-known-city)
    (cond (contains? (set edge) :cops)
          (print "You ran into the cops. Game Over.")

          (contains? node :wumpus)
          (if is-charging
            (println "You found the Wumpus!")
            (println "You ran into the Wumpus"))

          is-charging
          (println "You wasted your last bullet. Game Over.")

          has-worm
          (let [new-pos (random-node)]
            (printf "You ran into a Glow Worm Gang! You're now at %s\n" new-pos)
            (recur nil new-pos false)))))
