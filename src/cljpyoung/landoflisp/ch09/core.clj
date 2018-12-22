(ns cljpyoung.landoflisp.ch09.core
  (:require [cljpyoung.landoflisp.ch09.i-monster :as i-monster])
  (:require [cljpyoung.landoflisp.common.rand :as rand]))

;; const
(def MONSTER_NUM 12)

;; variables
;; TODO player {:health :agility} ...
(def &player-health (ref 0))
(def &player-agility (ref 0))
(def &player-strength (ref 0))
(def &monsters (atom []))
(def &monster-builders (atom []))
(def &random (atom (rand/->random 100)))


;; functions
(defn random [x]
  (binding [rand/*rand* @&random]
    (rand/rand-int x)))

(defn randval [n]
  (inc (random (max 1 n))))

(def fresh-line println)

;; ==============================
;; fn - player
;; ==============================

(defn init-player []
  (dosync
   (ref-set &player-health 30)
   (ref-set &player-agility 30)
   (ref-set &player-strength 30)))

(defn player-dead []
  (not (pos? @&player-health)))

(defn show-player []
  (fresh-line)
  (print "You are a valiant knight with a health of ")
  (print @&player-health)
  (print ", an agility of ")
  (print @&player-agility)
  (print ", and a strength of ")
  (print @&player-strength))

(declare monster-hit)
(declare monsters-dead)
(declare pick-monster)
(declare random-monster)
(defn player-attack []
  (fresh-line)
  (println "Attack style: [s]tab [d]ouble swing [r]oundhouse:")
  (flush)
  (case (read)
    s (monster-hit (pick-monster)
                   (+ 2 (randval (quot @&player-strength 2))))
    d (let [x (randval (quot @&player-strength 6))]
        (print "Your double swing has a strength of ")
        (print x)
        (fresh-line)
        (monster-hit (pick-monster) x)
        (when-not (monsters-dead)
          (monster-hit (pick-monster) x)))

    (dotimes [x (inc (randval (quot @&player-strength 3)))]
      (when-not (monsters-dead)
        (monster-hit (random-monster) 1)))))




;; ==============================
;; fn - monster
;; ==============================
(defn init-monsters []
(let [builders @&monster-builders
      builder-cnt (count builders)
      monsters (->> (fn [] (apply (get builders (random builder-cnt)) []))
                    (repeatedly MONSTER_NUM)
                    (vec))]
  (reset! &monsters monsters)))

(defn monster-health [m]
  @(:_&health m))

(defn monster-dead [m]
  (<= (monster-health m) 0))

(defn monsters-dead []
  (every? monster-dead @&monsters))

(defn monster-show [m]
  (print "A fierce ")
  (print (.getSimpleName (type m))))

(defn monster-hit [m x]
  (dosync (alter (:_&health m) - x))
  (if (monster-dead m)
    (do (print "You killed the ")
        (print (.getSimpleName (type m)))
        (print "! "))
    (do (print "You hit the ")
        (print (.getSimpleName (type m)))
        (print ", knocking off ")
        (print x)
        (print " health points! "))))

(defn monster-attack [m target]
  (i-monster/attack m target))

(defn show-monsters []
  (fresh-line)
  (print "Your foes:")
  (doseq [[x m] (map-indexed vector @&monsters)]
    (fresh-line)
    (print "   ")
    (print (inc x))
    (print ". ")
    (if (monster-dead m)
      (print "**dead**")
      (do (print "(Health=")
          (print (monster-health m))
          (print ") ")
          (monster-show m)))))


(defn random-monster []
  (let [monsters @&monsters
        m (get monsters (random (count monsters)))]
    (if-not (monster-dead m)
      m
      (recur))))

(defn pick-monster []
  (fresh-line)
  (print "Monster #:")
  (let [monsters @&monsters
        x (read)]
    (if (not (and (int? x) (>= x 1) (<= x MONSTER_NUM)))
      (do (print "That is not a valid monster number.")
          (recur))
      (let [m (get monsters (dec x))]
        (if (monster-dead m)
          (do (print "That monster is alread dead.")
              (recur))
          m)))))


;; =======================================
;; monster
;; =======================================

(defrecord Orc [_&health _club-level]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this target]
    (let [x (randval _club-level)]
      (print "An orc swings his club at you and knocks off ")
      (print x)
      (print " of your health points. ")
      (dosync (alter &player-health - x))))
  (show [this]
    (print "A wicked orc with a level ")
    (print _club-level)
    (print " club")))

(defrecord Hydra [_&health]
  i-monster/IMonster
  (hit [this damage]
    (dosync (alter _&health - damage))
    (if (monster-dead this)
      (print "The corpse of the fully decapitated and decapacitated hydra falls to the floor!")
      (do (print "You lop off ")
          (print damage)
          (print " of the hydra's heads! "))))
  (attack [this target]
    (let [x (randval (quot @_&health 2))]
      (print "A hydra attacks you with ")
      (print x)
      (print " of its hads! It also grows back one more head! ")
      (dosync
       (alter _&health inc)
       (alter &player-health - x))))
  (show [this]
    (print "A malicious hydra with ")
    (print @_&health)
    (print " heads.")))

(defrecord Slime [_&health _sliminess]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this target]
    (let [x (randval _sliminess)]
      (print "A slime mold wraps around your legs and decreases your agility by ")
      (print x)
      (print "! ")
      (dosync (alter &player-agility - x))
      (when (zero? (random 2))
        (print "It also squirts in your face, taking away a health point! ")
        (dosync (alter &player-health dec)))))
  (show [this]
    (print "A slime mold with a sliminess of ")
    (print _sliminess)))

(defrecord Brigand [_&health]
  i-monster/IMonster
  (hit [this damage]
    (monster-hit this damage))
  (attack [this target]
    (let [player-health @&player-health
          player-agility @&player-agility
          player-strength @&player-strength
          x (max player-health player-agility player-strength)]
      (cond (= x player-health)
            (do (print "A brigand hits you with his slingshot, taking off 2 health points! ")
                (dosync (alter &player-health - 2)))

            (= x player-agility)
            (do (print "A brigand catches your leg with his whip, taking off 2 agility points! ")
                (dosync (alter &player-agility - 2)))

            (= x player-strength)
            (do (print "A brigand cuts your arm with his whip, taking off 2 strength points! ")
                (dosync (alter &player-strength - 2))))))
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

;; =======================================
;; game
;; =======================================
(defn game-loop []
  (when-not (or (player-dead) (monsters-dead))
    (show-player)
    (dotimes [k (inc (quot (max 0 @&player-agility) 15))]
      (when-not (monsters-dead)
        (show-monsters)
        (player-attack)))
    (fresh-line)
    (doseq [m @&monsters]
      (when-not (monster-dead m)
        (monster-attack m nil)
        (fresh-line)))
    (game-loop)))

(defn orc-battle []
  (reset! &monster-builders [->orc ->hydra ->slime ->brigand])
  (init-monsters)
  (init-player)
  (game-loop)
  (when (player-dead)
    (println "You have been killed. Game Over."))
  (when (monsters-dead)
    (println "Congratulations! You have vanquished all of your foes.")))
