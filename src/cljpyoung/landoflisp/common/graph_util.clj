(ns cljpyoung.landoflisp.common.graph-util)

(def WIZARD_NODES {:living-room "you are in the living-room. a wizard is snoring loudly on the couch."
                   :garden "you are in a beautiful garden. there is a well in front of you."
                   :attic "you are in the attic. there is a giant welding torch in the corner."})

(def WIZARD_EDGES {:living-room {:garden [:west :door]
                                 :attic [:upstairs :ladder]}
                   :garden {:living-room [:east :door]}
                   :attic {:living-room [:downstairs :ladder]}})

(def MAX_LABEL_LENGTH 30)

(let [available-set (set "abcdefghijklmnopqrstuvwxyz1234567890")]
  (defn available? [c]
    (contains? available-set c)))

(defn dot-name [exp]
  (->> exp
       (str)
       (reduce (fn [^StringBuilder acc x]
                 (.append acc (if (available? x) x \_)))
               (StringBuilder.))
       (str)))

(defn dot-label [exp]
  (if-not exp
    ""
    (let [s (str exp)]
      (if (<= (count s) MAX_LABEL_LENGTH)
        s
        (str (->> s
                  (take (- MAX_LABEL_LENGTH 3))
                  (apply str))
             "...")))))

(defn nodes->dot [nodes]
  (doseq [node nodes]
    (printf "%s[label=\"%s\"];\n"
            (dot-name (first node))
            (dot-label (str (first node) "-" (second node))))))

(defn edges->dot [edges]
  (doseq [node edges]
    (let [[from es] node]
      (doseq [[to info] es]
        (printf "%s->%s[label=\"%s\"];\n"
                (dot-name from)
                (dot-name to)
                (dot-label (apply str (map str info))))))))

(defn graph->dot [nodes edges]
  (println "digraph{")
  (nodes->dot nodes)
  (edges->dot edges)
  (println "}"))

(defn dot->png [fname thunk]
  (spit fname (with-out-str (thunk)))
  (.exec (Runtime/getRuntime) (str "dot -Tpng -O " fname)))

(defn graph->png [fname nodes edges]
  (dot->png fname #(graph->dot nodes edges)))

(defn uedges->dot [edges]
  (loop [[fst & rst] edges]
    (when fst
      (let [[from dests] fst
            rst (into {} rst)]
        (doseq [[to info] dests]
          (when-not (contains? rst to)
            (println)
            (print (dot-name from))
            (print "--")
            (print (dot-name to))
            (print "[label=\"")
            (print (dot-label (apply str (map str info))))
            (print "\"];")))
        (recur rst)))))

(defn ugraph->dot [nodes edges]
  (println "graph{")
  (nodes->dot nodes)
  (uedges->dot edges)
  (println "}"))

(defn ugraph->png [fname nodes edges]
  (dot->png fname #(ugraph->dot nodes edges)))

;; (uedges->dot *wizard-edges*)
;; (graph->png "wizard.dot" *wizard-nodes* *wizard-edges*)
;; (graph->png "wizard.dot" *wizard-nodes* *wizard-edges*)
