(ns cljpyoung.landoflisp.ch15.core
  (:require [cljpyoung.landoflisp.common.rand :as rand]))

;; Dice of Doom
(def NUM_PLAYERS 2)
(def MAX_DICE 3)
(def BOARD_SIZE 3)
(def BOARD_HEXNUM (* BOARD_SIZE BOARD_SIZE))

;; data structure
;; tree {:player int
;;       :board [[player dice]]
;;       :moves []}
;; move {:action ([from to] | nil) :tree}

(def &random (atom (rand/->random 100)))
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))

;; functions
(defn gen-board
  ([] (gen-board (rand-int 100)))
  ([seed]
   (reset! &random (rand/->random seed))
   (->> #(vector (random NUM_PLAYERS) (inc (random MAX_DICE)))
        (repeatedly  BOARD_HEXNUM)
        (vec))))

(defn player-letter
  "
  > (player-letter 1)
  ;;=> \\b"
  [n]
  (char (+ n (int \a))))

(defn draw-board
  "
  > (draw-board [[0 3] [0 3] [1 3] [1 1]])
  ;;>>
  ;;>>   a-3 a-3
  ;;>>  b-3 b-1
  ;;=> nil"
  [board]
  (dotimes [y BOARD_SIZE]
    (println)
    (dotimes [_ (- BOARD_SIZE y)]
      (print " "))
    (dotimes [x BOARD_SIZE]
      (let [hex (get board (+ x (* BOARD_SIZE y)))
            [a b] hex]
        (printf "%s-%s " (player-letter a) b)))))

(defn neighbors'
  "
  | 0 1|
  |2 3 |
  > (neighbors 2)
  ;;=> #{0 3}"
  [pos]
  (let [up (- pos BOARD_SIZE)
        down (+ pos BOARD_SIZE)]
    (->> [up down]
         (concat
          (when-not (zero? (mod pos BOARD_SIZE)) [(dec up) (dec pos)])
          (when-not (zero? (mod (inc pos) BOARD_SIZE)) [(inc pos) (inc down)]))
         (filter (fn [p] (and (>= p 0) (< p BOARD_HEXNUM))))
         (set))))
(def neighbors (memoize neighbors'))

(defn board-attack
  "
  > (board-attack [[0 3] [0 3] [1 3] [1 1]] 0 1 3 3)
  ;;=> [[0 3] [0 1] [1 3] [0 2]]"
  [board player src dst dice]
  (vec (for [[pos hex] (map-indexed vector board)]
         (cond (= pos src) [player 1]
               (= pos dst) [player (dec dice)]
               :else hex))))

(defn add-new-dice
  "
  > (add-new-dice [[0 1] [1 3] [0 2] [1 1]] 0 2)
  ;;=> [[0 2] [1 3] [0 3] [1 1]]"
  [board player spare-dice]
  (loop [acc [], lst board, n spare-dice]
    (cond (nil? lst) (vec acc)
          (zero? n) (vec (concat acc lst))
          :else
          (let [[fst & rst] lst, [cur-player cur-dice] fst]
            (if (and (= cur-player player)
                     (< cur-dice MAX_DICE))
              (recur (conj acc [cur-player (inc cur-dice)]) rst (dec n))
              (recur (conj acc fst) rst n))))))

(declare add-passing-move)
(declare attacking-moves)
(defn game-tree'
  "
  > (game-tree [[0 1] [1 1] [0 2] [1 1]] 0 0 true)
  ;;=> {:player 0, :board [[0 1] [1 1] [0 2] [1 1]], :moves [{:action [2 3], :tree {:player 0, :board [[0 1] [1 1] [0 1] [0 1]], :moves [{:player 1, :board [[0 1] [1 1] [0 1] [0 1]], :moves []}]}}]}
  "
  [board player spare-dice is-first-move]
  {:player player
   :board board
   :moves (add-passing-move board
                            player
                            spare-dice
                            is-first-move
                            (attacking-moves board player spare-dice))})
(def game-tree (memoize game-tree'))

(defn attacking-moves
  "
  > (attacking-moves [[0 1] [1 1] [0 2] [1 1]] 0 0)
  ;;=> [{:action [2 3], :tree {:player 0, :board [[0 1] [1 1] [0 1] [0 1]], :moves [{:player 1, :board [[0 1] [1 1] [0 1] [0 1]], :moves []}]}}]
  "
  [board cur-player spare-dice]
  (letfn [(player [pos] (first (get board pos)))
          (dice [pos] (second (get board pos)))
          (vec2 [col] (when-let [seq (seq col)] (vec seq)))]
    (->> (range BOARD_HEXNUM)
         (filter #(= (player %) cur-player))
         (mapcat (fn [src]
                   (->> (neighbors src)
                        (filter (fn [dst] (and (not= (player dst) cur-player) (> (dice src) (dice dst)))))
                        (map (fn [dst]
                               {:action [src dst]
                                :tree (game-tree (board-attack board cur-player src dst (dice src))
                                                 cur-player
                                                 (+ spare-dice (dice dst))
                                                 false)})))))
         (filter (fn [x] (>= (count x) 1)))
         (vec))))

(defn add-passing-move
  "
  > (add-passing-move [[0 1] [1 1] [0 1] [0 1]] 0 2 false [])
  ;;=> [{:player 1, :board [[0 2] [1 1] [0 1] [0 1]], :moves []}]
  "
  [board player spare-dice is-first-move moves]
  (if is-first-move
    moves
    (if-let [tree (game-tree (add-new-dice board player (dec spare-dice))
                             (mod (inc player) NUM_PLAYERS)
                             0
                             true)]
      (conj moves {:action nil :tree tree})
      moves)))

(defn print-info [tree]
  (println)
  (let [{:keys [player board]} tree]
    (printf "current player = %s" (player-letter player))
    (draw-board board)))

(defn winners
  "
  > (winners [[0 1] [1 1] [0 2] [1 1]])
  ;;=> (0 1)"
  [board]
  (let [freq (frequencies (map first board))
        best (apply max (vals freq))]
    (->> freq
         (filter #(= (second %) best))
         (map first))))

(defn announce-winner
  "
  > (announce-winner [[0 1] [1 1] [0 2] [1 1]])
  ;;>>
  ;;>> The game is a tie between [\\a \\b]
  ;;=> nil"
  [board]
  (println)
  (let [ws (winners board)]
    (if (> (count ws) 1)
      (printf "The game is a tie between %s" (mapv player-letter ws))
      (printf "The winner is %s" (player-letter (first ws))))))

(defn handle-human
  [tree]
  (println)
  (print "choose your move:")
  (let [{:keys [moves]} tree]
    (doseq [[n move] (map-indexed vector moves)]
      (let [n (inc n)
            action (:action move)]
        (println)
        (printf "%s. " n)
        (if action
          (printf "%s -> %s" (first action) (second action))
          (print "end turn"))))
    (println)
    (->> (read) (dec) (get moves) :tree)))

(defn play-vs-human [tree]
  (print-info tree)
  (let [{:keys [board moves]} tree]
    (if-not (pos? (count moves))
      (announce-winner board)
      (recur (handle-human tree)))))


(declare get-ratings)
(defn rate-position' [tree p]
  (let [{:keys [moves player board]} tree]
    (if (pos? (count moves))
      (->> (get-ratings tree p)
           (apply (if (= player p) max min)))
      (let [w (winners board)]
        (if (some #{p} w)
          (/ 1 (count w))
          0)))))
(def rate-position (memoize rate-position'))

(defn get-ratings [tree player]
  (->> tree
       :moves
       (mapv #(rate-position (:tree %) player))))

(defn handle-computer [tree]
  (let [{:keys [player moves]} tree]
    (->> (get-ratings tree player)
         (map-indexed vector)
         (apply min-key second)
         (first)
         (get moves)
         :tree)))

(defn play-vs-computer [tree]
  (print-info tree)
  (let [{:keys [player board moves]} tree]
    (cond (zero? (count moves))
          (announce-winner board)

          (= player 0)
          (play-vs-computer (handle-human tree))

          :else
          (play-vs-computer (handle-computer tree)))))

;; (play-vs-human (game-tree [[1 2] [1 2] [0 2] [1 1]] 0 0 true))
;; (play-vs-computer (game-tree [[1 2] [1 2] [0 2] [1 1]] 0 0 true))
