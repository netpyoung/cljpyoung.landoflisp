(ns cljpyoung.landoflisp.ch19.core
  (:refer-clojure :exclude [rand])
  (:require
   [cljpyoung.svg :as svg :include-macros true]
   [cljpyoung.landoflisp.common.rand :as rand]))

;; Dice of Doom. v3
(def DICE_SCALE 40)
(def DOT_SIZE 0.05)
(def BOARD_SCALE 64)
(def TOP_OFFSET 3)
(def DIE_COLORS [[255 63 63] [63 63 255]])

(def NUM_PLAYERS 2)
(def MAX_DICE 2)
(def BOARD_SIZE 4)
(def BOARD_HEXNUM (* BOARD_SIZE BOARD_SIZE))
(def AI_LEVEL 4)


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

(defn draw-die-svg [x y col]
  (letfn [(calc-pt [[px py]]
            [(+ x (* DICE_SCALE px))
             (+ y (* DICE_SCALE py))])
          (f [pol col]
            (svg/polygon (mapv calc-pt pol) col))]
    [:g
     (f [[0 -1] [-0.6 -0.75] [0 -0.5] [0.6 -0.75]] (svg/brightness col 40))
     (f [[0 -0.5] [-0.6 -0.75] [-0.6 0] [0 0.25]] col)
     (f [[0 -0.5] [0.6 -0.75] [0.6 0] [0 0.25]] (svg/brightness col -40))
     (mapv (fn [x y]
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
   (for [y (range BOARD_SIZE)
         x (range BOARD_SIZE)]
     (let [pos (+ x (* BOARD_SIZE y))
           hex (get board pos)
           xx (* BOARD_SCALE (+ (* 2 x) (- BOARD_SIZE y)))
           yy (* BOARD_SCALE (+ (* y 0.7) TOP_OFFSET))
           col (svg/brightness (nth DIE_COLORS (first hex))
                               (* -15 (- BOARD_SIZE y)))]
       (if (some #{pos} legal-tiles)
         [:g
          [:a {:href (make-game-link pos)}
           (draw-tile-svg x y pos hex xx yy col chosen-tile)]]
         (draw-tile-svg x y pos hex xx yy col chosen-tile))))])
