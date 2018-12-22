(ns cljpyoung.landoflisp.ch09.i-monster)

(defprotocol IMonster
  (hit [this damage])
  (attack [this target])
  (show [this]))
