(def project
  {:project     'netpyoung/cljpyoung.landoflisp
   :version     "0.1.0"
   :description "FIXME: write description"
   :url         "https://github.com/netpyoung/cljpyoung.landoflisp"
   :scm         {:url "https://github.com/netpyoung/cljpyoung.landoflisp"}
   :license     {"GNU Free Documentation License 1.3"
                 "https://www.gnu.org/licenses/fdl.txt"}})

(set-env!
 :source-paths #{"src" "env/dev"}
 :resource-paths #{"resources/public"}

 ;; https://okky.kr/article/424622
 ;; https://cloudplatform.googleblog.com/2015/11/faster-builds-for-Java-developers-with-Maven-Central-mirror.html
 ;; set BOOT_MAVEN_CENTRAL_MIRROR=https://maven-central-asia.storage-download.googleapis.com/repos/central/data/
 :mirrors
 {#"maven-central"
  {:name "google"
   :url "https://maven-central-asia.storage-download.googleapis.com/repos/central/data/"}
  ;; #"clojar"
  ;; {:name "x"
  ;;  :url "https://clojars.org/repo/"}
  }

 :dependencies
 '[[org.clojure/clojure "1.10.0"]

   ;; clojurescript
   [org.clojure/clojurescript "1.10.439"]

   ;; reagent
   [reagent "0.7.0" :exclusions [cljsjs/react]]
   [cljsjs/react "16.0.0-0"]
   [cljsjs/react-dom "16.0.0-0"]
   [cljsjs/react-with-addons "15.4.2-2"]


   [adzerk/boot-cljs "2.1.5" :scope "test"]

   ;; ref: https://github.com/adzerk-oss/boot-reload
   [adzerk/boot-reload "0.6.0" :scope "test"]

   [powerlaces/boot-figreload "0.5.14" :scope "test"]
   [pandeiro/boot-http "0.7.6" :scope "test"]

   ;; clojurescript repl
   ;; ref: https://github.com/adzerk-oss/boot-cljs-repl
   [adzerk/boot-cljs-repl   "0.4.0"] ;; latest release
   [cider/piggieback        "0.3.9"  :scope "test"]
   [weasel                  "0.7.0"  :scope "test"]
   [nrepl                   "0.4.5"  :scope "test"]

   ;; figwheel
   ;; ref: https://github.com/boot-clj/boot-figreload
   [powerlaces/boot-figreload "0.5.14" :scope "test"]

   ;; Dirac and cljs-devtoos
   ;; ref: https://github.com/binaryage/dirac
   ;; ref: https://github.com/binaryage/cljs-devtools
   [binaryage/dirac "1.3.0" :scope "test"]
   [binaryage/devtools "0.9.10" :scope "test"]
   [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]

   [adzerk/boot-test "RELEASE" :scope "test"]])


(deftask check []
  ;; ref: https://github.com/tolitius/boot-check
  (set-env! :dependencies #(conj % '[tolitius/boot-check "0.1.11" :scope "test"]))
  (require '[tolitius.boot-check])
  (let [with-yagni (resolve 'tolitius.boot-check/with-yagni)
        with-eastwood (resolve 'tolitius.boot-check/with-eastwood)
        with-kibit (resolve 'tolitius.boot-check/with-kibit)
        with-bikeshed (resolve 'tolitius.boot-check/with-bikeshed)]
    (comp
     (with-yagni)
     (with-eastwood)
     (with-kibit)
     (with-bikeshed))))

(deftask bat-test
  []
  (set-env! :dependencies #(conj % '[metosin/bat-test "0.4.0" :scope "test"]))
  (set-env! :dependencies #(conj % '[org.clojure/tools.namespace "0.3.0-alpha4" :exclusions [org.clojure/clojure] :scope "test"]))
  (require '[metosin.bat-test])
  (let [bat-test (resolve 'metosin.bat-test/bat-test)]
    (bat-test)))


(deftask build
  [_ snapshot LOCAL boolean "build local"]
  (task-options!
   pom (if snapshot
         (update-in project [:version] (fn [x] (str x "-SNAPSHOT")))
         project))
  (comp (pom)))

(deftask local
  []
  (comp (build)
        (jar)
        (install)))

(deftask local-snapshot
  []
  (comp (build :snapshot true)
        (jar)
        (install)))

(deftask prepare-push
  []
  (set-env!
   :repositories
   #(conj % ["clojars" {:url "https://clojars.org/repo/"
                        :username (get-sys-env "CLOJARS_USER" :required)
                        :password (get-sys-env "CLOJARS_PASS" :required)}]))
  identity)

(deftask push-release
  []
  (comp (prepare-push)
        (build)
        (jar)
        (push :repo "clojars" :ensure-release true)))

(deftask push-snapshot
  []
  (comp (prepare-push)
        (build :snapshot true)
        (jar)
        (push :repo "clojars" :ensure-snapshot true)))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (with-pass-thru fs
    (require '[cljpyoung.landoflisp :as app])
    (apply (resolve 'app/-main) args)))

(require '[adzerk.boot-cljs :refer [cljs]])
(require '[adzerk.boot-reload :refer [reload]])
(require '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])
(require '[pandeiro.boot-http :refer [serve]])
(require '[powerlaces.boot-cljs-devtools :refer [dirac cljs-devtools]])
(deftask dev []
  (comp
   (serve :port 8080 :httpkit true)
   (watch)
   (reload)
   (cljs-repl)
   (cljs :ids #{"js\\main"})))

(require '[adzerk.boot-test :refer [test]])
