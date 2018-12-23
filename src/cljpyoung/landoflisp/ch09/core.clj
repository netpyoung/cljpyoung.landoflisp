(ns cljpyoung.landoflisp.ch09.core
  (:require [cljpyoung.landoflisp.ch09.i-monster :as i-monster])
  (:require [cljpyoung.landoflisp.common.rand :as rand]))

;; const
(def MONSTER_NUM 12)

;; variables
(defrecord Player [health agility strength])
(def &player (ref (->Player 30 30 30)))
(def &random (atom (rand/->random 100)))

;; functions
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))

(defn randval [n]
  (inc (random (max 1 n))))

;; =======================================
;; monster
;; =======================================
(defn monster-health [m]
  @(:_&health m))

(defn monster-dead [m]
  (not (pos? (monster-health m))))

(defn monster-name [m]
  (.getSimpleName (type m)))

(defn monster-show [m]
  (printf "A fierce %s" (monster-name m)))

(defn monster-attack [m &p]
  (i-monster/attack m &p))

(defn monster-hit [m x]
  (dosync (alter (:_&health m) - x))
  (if (monster-dead m)
    (printf "You killed the %s!" (monster-name m))
    (printf "You hit the %s, knocking off %s health points!" (monster-name m) x)))

(defrecord Orc [_&health _club-level]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this &p]
    (let [x (randval _club-level)]
      (printf "An orc swings his club at you and knocks off %s of your health points. " x)
      (dosync (alter &p update :health - x))))
  (show [this]
    (printf "A wicked orc with a level %s club." _club-level)))

(defrecord Hydra [_&health]
  i-monster/IMonster
  (hit [this damage]
    (dosync (alter _&health - damage))
    (if (monster-dead this)
      (printf "The corpse of the fully decapitated and decapacitated hydra falls to the floor!")
      (printf "You lop off %s of the hydra's heads! " damage)))
  (attack [this &p]
    (let [x (randval (quot @_&health 2))]
      (printf "A hydra attacks you with %s of its hads! It also grows back one more head! " x)
      (dosync
       (alter _&health inc)
       (alter &p update :health - x))))
  (show [this]
    (printf "A malicious hydra with %s heads." @_&health)))


(defrecord Slime [_&health _sliminess]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this &p]
    (let [x (randval _sliminess)]
      (printf "A slime mold wraps around your legs and decreases your agility by %s! " x)
      (dosync (alter &p update :agility - x))
      (when (zero? (random 2))
        (print "It also squirts in your face, taking away a health point! ")
        (dosync (alter &p update :health dec)))))
  (show [this]
    (printf "A slime mold with a sliminess of %s" _sliminess)))


(defrecord Brigand [_&health]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this &p]
    (let [{:keys [health agility strength]} @&p
          x (max health agility strength)]
      (cond (= x health)
            (do (print "A brigand hits you with his slingshot, taking off 2 health points! ")
                (dosync (alter &p update :health - 2)))
            (= x agility)
            (do (print "A brigand catches your leg with his whip, taking off 2 agility points! ")
                (dosync (alter &p update :agility - 2)))
            (= x strength)
            (do (print "A brigand cuts your arm with his whip, taking off 2 strength points! ")
                (dosync (alter &p update :strength - 2))))))
  (show [this]
    (monster-show this)))

(defn ->orc []
  (->Orc (ref (randval 10)) (randval 8)))
(defn ->hydra []
  (->Hydra (ref (randval 10))))
(defn ->slime []
  (->Slime (ref (randval 10)) (randval 5)))
(defn ->brigand []
  (->Brigand (ref (randval 10))))

;; ==============================
;; fn - monster
;; ==============================
(defn init-monsters [builders monster-count]
  (let [builder-cnt (count builders)]
    (->> (fn [] (apply (get builders (random builder-cnt)) []))
         (repeatedly monster-count)
         (vec))))

(defn monsters-dead [ms]
  (every? monster-dead ms))

(defn show-monsters [ms]
  (println)
  (print "Your foes:")
  (doseq [[x m] (map-indexed vector ms)]
    (println)
    (printf "   %s. " (inc x))
    (if (monster-dead m)
      (print "**dead**")
      (do (printf "(Health=%s) " (monster-health m))
          (monster-show m)))))

(defn random-monster [monsters]
  (let [m (get monsters (random (count monsters)))]
    (if-not (monster-dead m)
      m
      (recur monsters))))

(defn pick-monster [monsters]
  (println)
  (print "Monster #:")
  (flush)
  (let [x (read)]
    (if (not (and (int? x) (>= x 1) (<= x MONSTER_NUM)))
      (do (print "That is not a valid monster number.")
          (recur monsters))
      (let [m (get monsters (dec x))]
        (if (monster-dead m)
          (do (print "That monster is alread dead.")
              (recur monsters))
          m)))))

;; ==============================
;; fn - player
;; ==============================
(defn init-player [&p]
  (->> (map->Player {:health 30 :agility 30 :strength 30})
       (ref-set &p)
       (dosync)))

(defn player-dead [p]
  (not (pos? (:health p))))

(defn show-player [p]
  (println)
  (let [{:keys [health agility strength]} p]
    (printf "You are a valiant knight with a health of %s, an agility of %s, and a strength of %s" health agility strength)))

(defn player-attack [p m]
  (println)
  (print "Attack style: [s]tab [d]ouble swing [r]oundhouse:")
  (flush)
  (let [{:keys [strength]} p]
    (case (read)
      s (monster-hit (pick-monster m) (+ 2 (randval (quot strength 2))))

      d (let [x (randval (quot strength 6))]
          (printf "Your double swing has a strength of %s" x)
          (println)
          (monster-hit (pick-monster m) x)
          (when-not (monsters-dead m)
            (monster-hit (pick-monster m) x)))

      (dotimes [x (inc (randval (quot strength 3)))]
        (when-not (monsters-dead m)
          (monster-hit (random-monster m) 1))))))

;; =======================================
;; game
;; =======================================
(defn game-loop [&p ms]
  (when-not (or (player-dead @&p) (monsters-dead ms))
    (show-player @&p)
    (dotimes [k (inc (quot (max 0 (:agility @&p)) 15))]
      (when-not (monsters-dead ms)
        (show-monsters ms)
        (player-attack @&p ms)))
    (println)
    (doseq [m ms]
      (when-not (monster-dead m)
        (monster-attack m &p)
        (println)))
    (recur &p ms)))

(defn orc-battle []
  (init-player &player)
  (let [monsters (init-monsters [->orc ->hydra ->slime ->brigand] MONSTER_NUM)]
    (game-loop &player monsters)
    (cond (player-dead @&player)
          (println "You have been killed. Game Over.")
          (monsters-dead monsters)
          (println "Congratulations! You have vanquished all of your foes."))))
