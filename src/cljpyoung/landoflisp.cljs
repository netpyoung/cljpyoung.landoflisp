(ns cljpyoung.landoflisp
  (:require
   [cljpyoung.landoflisp.ch19.core :as ch19]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]))

;; =====================================
;; DB
;; =====================================
(def initial-db
  {:test           0
   :winner         nil
   :from-tile      nil
   :chosen         nil
   :curr-game-tree (ch19/game-tree (ch19/gen-board 1) 0 0 true)})

;; =====================================
;; Events
;; =====================================
(rf/reg-event-db
  :evt-init-game
  (fn-traced [_ _]
    initial-db))

(rf/reg-event-db
  :evt-start-game
  (fn-traced [db _]
    db))

(rf/reg-event-db
  :test
  (fn-traced [db [_ test-val]]
    (update-in db [:test] + test-val)))

(rf/reg-event-db
  :evt-user-select
  (fn-traced [db [_ [chosen-tile pos]]]
    (assoc db :chosen chosen-tile)))


;; =====================================
;; Sub
;; =====================================
(rf/reg-sub
  :get-test
  (fn [db _]
    (:test db)))

(rf/reg-sub
  :get-game-state
  (fn [db _] db))


;; =====================================
;; Logic
;; =====================================
(declare main)
(declare game)

(defonce click-count (r/atom 0))

(defn on-js-reload
  []
  (enable-console-print!)
  (main)
  (println "on-js-reload"))

(defn app-root
  []
  [:div
   (game @(rf/subscribe [:get-game-state]))

   (let [{:keys [curr-game-tree from-tile]} @(rf/subscribe [:get-game-state])]
     (when curr-game-tree
       [ch19/draw-dod-page curr-game-tree from-tile]))

   [:button
    {:on-click #(rf/dispatch [:test 1])}]
   [:h2 "hello" @click-count]
   [:h2 "hello" @(rf/subscribe [:get-test])]
   ])

(defn announce-winner
  [player]
  (js/alert player))

(defn handle-human
  [chosen]
  (js/alert chosen))

(defn handle-computer
  []
  (println "handle-computer"))

(defn game
  [db]
  (let [{:keys [curr-game-tree from-tile chosen]} db
        {:keys [player board moves]}              curr-game-tree]
    (enable-console-print!)
    (println curr-game-tree)
    (cond
      ;; check winner
      (= (count moves) 0)
      (announce-winner player)

      ;; player1 turn.
      (= player 0)
      (handle-human chosen)

      :else
      (handle-computer))))



;; :compuyer-action
;; (handle-computer *cur-game-tree*)

;; :player-action chosen
;; (web-handle-human)

;; (defn web-handle-human [pos]
;;   (cond (not pos)
;;         (princ "Please choose a hex to move from:")

;;         (= pos 'pass)
;;         (do (setf *cur-game-tree*
;;                   (cadr (lazy-car (caddr *cur-game-tree*))))
;;             (princ "Your reinforcements have been placed.")
;;             (tag a (href (make-game-link nil))
;;                  (princ "continue")))
;;         (not *from-tile*)
;;         (do (setf *from-tile* pos)
;;             (princ "Now choose a destination:"))

;;         (eq pos *from-tile*)
;;         (do (setf *from-tile* nil)
;;             (princ "Move cancelled."))

;;         :else
;;         (do (setf *cur-game-tree*
;;                   (cadr (lazy-find-if (lambda (move)
;;                                               (equal (car move)
;;                                                      (list *from-tile* pos)))
;;                                       (caddr *cur-game-tree*))))
;;             (setf *from-tile* nil)
;;             (princ "You may now ")
;;             (tag a (href (make-game-link 'pass))
;;                  (princ "pass"))
;;             (princ " or make another move:"))))

(defn ^:export main []
  (if-let [node (js/document.getElementById  "app")]
    (do (rf/dispatch-sync [:evt-init-game])
        (rf/dispatch-sync [:evt-start-game])
        (rdom/render [app-root] node))
    (js/alert "Fail to find `app`")))

(main)