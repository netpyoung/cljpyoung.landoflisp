(ns cljpyoung.landoflisp.ch19.core
  (:refer-clojure :exclude [rand])
  (:require
    [goog.string :as gstring]
    [goog.string.format :as format]

   [cljpyoung.svg :as svg :include-macros true]
   [cljpyoung.landoflisp.common.rand :as rand]))


;; Dice of Doom. v3
(def NUM_PLAYERS 2)
(def MAX_DICE 2)
(def BOARD_SIZE 4)
(def BOARD_HEXNUM (* BOARD_SIZE BOARD_SIZE))
(def AI_LEVEL 4)

(def INT_MAX Number.MAX_SAFE_INTEGER)
(def INT_MIN Number.MIN_SAFE_INTEGER)

;;
(def BOARD_WIDTH 900)
(def BOARD_HEIGHT 500)
(def BOARD_SCALE 64)
(def TOP_OFFSET 3)
(def DICE_SCALE 40)
(def DOT_SIZE 0.05)
(def DIE_COLORS [[255 63 63] [63 63 255]])



(def &cur-game-tree (atom nil))
(def &from-tile (atom nil))


;; data structure
;; tree {:player int
;;       :board [[player dice]]
;;       :moves [(delay move)]}
;; move {:action ([player dice] | nil) :tree}

(def &random (atom (rand/->random 100)))
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))


(def format gstring/format)
(defn printf
  "Prints formatted output, as per format"
  [fmt & args]
  (print (apply format fmt args)))

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

  ;; (int \a)
  ;; WARNING: cljs.core/bit-or, all arguments must be numbers, got [string number] instead at line 1 <cljs repl>
  (char (+ n (.charCodeAt \a 0))))

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
      (print "  "))
    (dotimes [x BOARD_SIZE]
      (let [[a b] (get board (+ x (* y BOARD_SIZE)))]
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
         (filter #(and (<= 0 %) (< % BOARD_HEXNUM)))
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
  > (attacking-moves [[1 2] [0 1] [0 1] [0 1] [1 1] [0 1] [1 2] [1 2] [1 1] [0 2] [0 1] [0 1] [0 2] [0 1] [0 1] [0 1]] 0 0)
  ;;=> ;;=> [#delay[{:status :pending, :val nil} 0x1299ce2] #delay[{:status :pending, :val nil} 0x89fdf6] #delay[{:status :pending, :val nil} 0xe373e4]]
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
                        (map (fn [dst] [src dst])))))
         (map (fn [[src dst]]
                (delay {:action [src dst]
                        :tree (game-tree (board-attack board cur-player src dst (dice src))
                                         cur-player
                                         (+ spare-dice (dice dst))
                                         false)
                        })))
         (vec))))

(defn add-passing-move
  "
  > (add-passing-move [[0 1] [1 1] [0 1] [0 1]] 0 2 false [])
  ;;=> [{:player 1, :board [[0 2] [1 1] [0 1] [0 1]], :moves []}]
  "
  [board player spare-dice is-first-move moves]
  (if is-first-move
    moves
    (conj moves
          (delay {:action nil
                  :tree (game-tree (add-new-dice board player (dec spare-dice))
                                   (mod (inc player) NUM_PLAYERS)
                                   0
                                   true)}))))


(defn print-info
  "
  > (print-info {:player 0, :board [[0 1] [1 1] [0 2] [1 1]], :moves [{:action [2 3], :tree {:player 0, :board [[0 1] [1 1] [0 1] [0 1]], :moves [{:player 1, :board [[0 1] [1 1] [0 1] [0 1]], :moves []}]}}]})
  ;;>>
  ;;>> current player = a
  ;;>>   a-1 b-1
  ;;>>  a-2 b-1
  ;;=> nil
  "
  [tree]
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
            action (:action (force move))]
        (println)
        (printf "%s. " n)
        (if action
          (let [[from to] action]
            (printf "%s -> %s" from to))
          (print "end turn"))))
    (println)
    (->> (read) (dec) (get moves) force :tree)))

(defn play-vs-human [tree]
  (print-info tree)
  (let [{:keys [board moves]} tree]
    (if-not (pos? (count moves))
      (announce-winner board)
      (recur (handle-human tree)))))

(declare get-ratings)
(declare threatened?)
(defn score-board [board player]
  (->> (map-indexed vector board)
       (map (fn [[pos [p _]]]
              (cond (not= p player) -1
                    (threatened? pos board) 1
                    :else 2)))
       (apply +)))

(defn threatened? [pos board]
  (let [[player dice] (get board pos)]
    (->> (neighbors pos)
         (some (fn [n]
                 (let [[nplayer ndice] (get board n)]
                   (and (not= player nplayer) (> ndice dice)))))
         (some?))))

(defn rate-position' [tree p]
  (let [{:keys [moves player board]} tree]
    (if-not (pos? (count moves))
      (score-board board player)
      (->> (get-ratings tree p)
           (apply (if (= player p) max min))))))

(def rate-position (memoize rate-position'))

(defn get-ratings [tree player]
  (->> tree
       :moves
       (mapv #(rate-position (:tree (force %)) player))))

(declare handle-computer)
;; (defn handle-computer [tree]
;;   (let [{:keys [player moves]} tree]
;;     (->> (get-ratings tree player)
;;          (map-indexed vector)
;;          (apply min-key second)
;;          (first)
;;          (get moves)
;;          force
;;          :tree)))

(defn play-vs-computer [tree]
  (print-info tree)
  (let [{:keys [player board moves]} tree]
    (cond (zero? (count moves))
          (announce-winner board)

          (= player 0)
          (recur (handle-human tree))

          :else
          (recur (handle-computer tree)))))

(defn limit-tree-depth [tree depth]
  (let [{:keys [player board moves]} tree]
    {:player player
     :board board
     :moves (if (zero? depth)
              []
              (->> moves
                   (mapv (fn [move]
                           (let [{:keys [action tree]} (force move)]
                             {:action action
                              :tree (limit-tree-depth tree (dec depth))})))))}))

;; (defn handle-computer [tree]
;;   (let [{:keys [player board moves]} tree]
;;     (->> (get-ratings (limit-tree-depth tree AI_LEVEL) player)
;;          (map-indexed vector)
;;          (apply min-key second)
;;          (first)
;;          (get moves)
;;          force
;;          :tree)))

;;       a-1 a-3 a-1 b-2
;;     b-3 a-3 a-3 a-1
;;   a-3 a-3 b-1 a-2
;; b-3 a-3 a-1 a-3
;; (-> [[0 1] [0 3] [0 1] [1 2]
;;      [1 3] [0 3] [0 3] [0 1]
;;      [0 3] [0 3] [1 1] [0 2]
;;      [1 3] [0 3] [0 1] [0 3]]
;;     (game-tree 0 0 true)
;;     (play-vs-computer))

;;       a-1 b-2 b-1 a-3
;;     b-3 a-1 a-3 a-3
;;   b-3 b-2 b-2 b-2
;; a-3 a-3 a-2 a-2
;; (-> [[0 1] [1 2] [1 1] [0 3]
;;      [1 3] [0 1] [0 3] [0 3]
;;      [1 3] [1 2] [1 2] [1 2]
;;      [0 3] [0 3] [0 2] [0 2]]
;;     (game-tree 0 0 true)
;;     (play-vs-computer))

;;         a-2 b-2 a-1 b-2 b-2
;;       a-1 b-2 b-3 b-3 a-3
;;     a-1 b-2 a-3 b-1 b-2
;;   b-1 b-3 a-2 b-2 a-1
;; b-3 b-1 b-1 a-3 b-3
;; (-> [[0 2] [1 2] [0 1] [1 2] [1 2]
;;      [0 1] [1 2] [1 3] [1 3] [0 3]
;;      [0 1] [1 2] [0 3] [1 1] [1 2]
;;      [1 1] [1 3] [0 2] [1 2] [0 1]
;;      [1 3] [1 1] [1 1] [0 3] [1 3]]
;;     (game-tree 0 0 true)
;;     (play-vs-computer))

(declare ab-get-ratings-max)
(declare ab-get-ratings-min)
(defn ab-rate-position [tree player upper-limit lower-limit]
  (let [moves (:moves tree)]
    (if (pos? (count moves))
      (if (= (:player tree) player)
        (apply max (ab-get-ratings-max tree player upper-limit lower-limit))
        (apply min (ab-get-ratings-min tree player upper-limit lower-limit)))
      (score-board (:board tree) player))))

(defn ab-get-ratings-min [tree player upper-limit lower-limit]
  (loop [acc []
         moves (:moves tree)
         upper-limit upper-limit]
    (if-not (pos? (count moves))
      acc
      (let [[fst & rst] moves]
        (let [x (ab-rate-position (:tree (force fst))
                                  player
                                  upper-limit
                                  lower-limit)]
          (if (<= x lower-limit)
            (conj acc x)
            (recur (conj acc x) rst (min x upper-limit))))))))

(defn ab-get-ratings-max [tree player upper-limit lower-limit]
  (loop [acc []
         moves (:moves tree)
         lower-limit lower-limit]
    (if-not (pos? (count moves))
      acc
      (let [[fst & rst] moves]
        (let [x (ab-rate-position (:tree (force fst))
                                  player
                                  upper-limit
                                  lower-limit)]
          (if (>= x upper-limit)
            (conj acc x)
            (recur (conj acc x) rst (max x lower-limit))))))))

(defn handle-computer [tree]
  (let [{:keys [player moves]} tree]
    (->> (ab-get-ratings-max (limit-tree-depth tree AI_LEVEL)
                             player
                             INT_MAX
                             INT_MIN)
         (map-indexed vector)
         (apply min-key second)
         (first)
         (get moves)
         force
         :tree)))

;; ==================
;; draw part
;; ==================
(defn draw-die-svg [x y col]
  (letfn [(calc-pt [[px py]]
            [(+ x (* DICE_SCALE px))
             (+ y (* DICE_SCALE py))])
          (f [pol col]
            (svg/polygon (mapv calc-pt pol) col))]
    ^{:key (str "darw-dice-root" [x y col])}
    [:g
     (f [[0 -1] [-0.6 -0.75] [0 -0.5] [0.6 -0.75]] (svg/brightness col 40))
     (f [[0 -0.5] [-0.6 -0.75] [-0.6 0] [0 0.25]] col)
     (f [[0 -0.5] [0.6 -0.75] [0.6 0] [0 0.25]] (svg/brightness col -40))
     (map (fn [x y]
            (svg/polygon (mapv (fn [xx yy]
                                 (calc-pt [(+ x (* xx DOT_SIZE))
                                           (+ y (* yy DOT_SIZE))]))
                               [-1 -1 1 1]
                               [-1 1 1 -1])
                         [255 255 255]))
          [-0.05  0.125 0.3 -0.3
           -0.125 0.05 0.2 0.2
           0.45 0.45 -0.45 -0.2]
          [-0.875 -0.80 -0.725 -0.775
           -0.70 -0.625 -0.35 -0.05
           -0.45 -0.15 -0.45 -0.05])]))

(defn draw-tile-svg [x y pos hex xx yy color chosen-tile]
  ^{:key (str "draw-tile-svg-root:" [x y pos hex xx yy])}
  [:g
   (for [z (range 2)]
     (svg/polygon (mapv (fn [[px py]]
                          [(+ xx (* BOARD_SCALE px)) (+ yy (* BOARD_SCALE (+ py (* (- 1 z) 0.1))))])
                        [[-1 -0.2] [0 -0.5] [1 -0.2] [1 0.2] [0 0.5] [-1 0.2]])
                  (if (= pos chosen-tile)
                    (svg/brightness color 100)
                    color)))
   (let [[_ dice] hex]
     (for [z (range dice)]
       (draw-die-svg (+ xx
                        (* DICE_SCALE
                           0.3
                           (if (odd? (+ x y z))
                             -0.3
                             0.3)))
                     (- yy (* DICE_SCALE z 0.8)) color)))])

(defn make-game-link [pos]
  (str "/game.html?chosen=" pos))

(defn draw-board-svg [board chosen-tile legal-tiles]
  [:g
   (for [y (range BOARD_SIZE), x (range BOARD_SIZE)]

     (let [pos (+ x (* BOARD_SIZE y))
           hex (get board pos)
           xx (* BOARD_SCALE (+ (* 2 x) (- BOARD_SIZE y)))
           yy (* BOARD_SCALE (+ (* y 0.7) TOP_OFFSET))
           col (svg/brightness (nth DIE_COLORS (first hex)) (* -15 (- BOARD_SIZE y)))]
       (if (some #{pos} legal-tiles)
         ^{:key (str "darw-board-" [x y])}
         [:g
          [:a {:href (make-game-link pos)}
           (draw-tile-svg x y pos hex xx yy col chosen-tile)]]

         ^{:key (str "darw-board-" [x y])}
         (draw-tile-svg x y pos hex xx yy col chosen-tile))))])

(defn draw-dod-page [tree selected-tile]
  (let [{:keys [board moves]} tree]
    (svg/svg
     BOARD_WIDTH BOARD_HEIGHT
     (draw-board-svg
      board
      selected-tile
      (if selected-tile
        (->> moves
             (map deref)
             (map (fn [move]
                    (let [[tile dice] move]
                      (when (= tile selected-tile)
                        dice)))))
        (->> moves
             (map deref)
             (map first)))))))

(defn test-page []
  (draw-dod-page
   (game-tree (gen-board) 0 0 true) nil))
