(ns clj-anti-z.core
  (:import [java.io File]
           [java.util.concurrent LinkedBlockingQueue])
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :refer [response]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.format-params :refer [wrap-restful-params]]
            [ring.middleware.format-response :refer [wrap-restful-response]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clj-yaml.core :as yaml])
  (:gen-class))


(def state-path (atom "./state.yml"))

(def cluster-state (atom {}))

(def  ^LinkedBlockingQueue event-queue (LinkedBlockingQueue.))

;;UTIL
(defn yaml->map
  [st]
  (yaml/parse-string st))

(defn map->yaml
  [mp]
  (yaml/generate-string mp :dumper-options {:flow-style :block :indent 2}))

(defn yaml-file->map
 [filepath]
 (yaml->map (slurp filepath)))

(defn map->yaml-file
  [filepath mp]
  (try
    (with-open [w (io/writer filepath)]
      (.write w (map->yaml mp)))
      (catch Exception e
        (log/info "Can't write file"))))

(defn mk-resp
  "easy ring response formatter"
  ([status state body]
    {:status status :body (assoc body :state state)})
  ([status state body msg]
    {:status status :body (assoc body :state state :msg (str msg))}))

(defn start-thread!
  "This function launch a thread"
  [user-fn name]
  (log/info "Thread " name " started")
  (let [run (atom true)
        t (Thread.
           (fn []
             (while @run
               (try
                 (user-fn)
                (catch java.lang.InterruptedException _
                  (log/info "receive an interrupt exception in thread loop, exit " name)
                  (reset! run false))
                (catch Throwable t
                  (log/fatal t "FATAL ERROR EXIT THREAD LOOP : " name)
                  (reset! run false))))
             (log/info "End of operation thread" name)))]
    (.setDaemon t true)
    (.start t)
    {:stop
     (fn []
       (reset! run false)
       (.join t 1000)
       (when (.isAlive t)
         (log/warn "thread loop still alive (blocking take), interrupt." name)
         (.interrupt t)
         (.join t 1000))
       (log/info "thread loop stopped." name))
     :status (fn [] (and @run (.isAlive t)))}))


;;Cluster function

(defn get-el-state
  [el]
  (get @cluster-state (keyword el) {}))

(defn set-el-state!
  [id state type ts info]
  (swap! cluster-state assoc (keyword id) {:state state
                                           :type type
                                           :ts ts
                                           :info info})
  (when (= 0 (.size event-queue))
    (map->yaml-file @cluster-state)))

(defn get-cluster-state
  ([]
    @cluster-state)
  ([state]
  (into {}(filter
    (fn [[k v]] (= state (:state v)))
      @cluster-state)))
  ([type state]
  (into {}(filter
    (fn [[k v]] (and (= state (:state v)) (= type (:type v))))
      @cluster-state))))

(defn send-cluster-event
  [id state type ts info]
  (.put event-queue [id state type ts info]))

(defroutes STATE
  (GET "/el/:id" [id] (mk-resp 200 "success" (get-el-state id)))
  (POST "/el/:id" [id state type ts info] (do (send-cluster-event id state type ts info)
                                 (mk-resp 200 "success" {} "Operation submitted")))
  (GET "/cluster" [] (mk-resp 200 "success" (get-cluster-state)))
  (GET "/els/:state" [state] (mk-resp 200 "success" (get-cluster-state state)))
  (GET "/els/:type/:state" [type state] (mk-resp 200 "success" (get-cluster-state type state))))

(defroutes app-routes
  (context "/state" [] STATE))

(def handler (-> app-routes
                 (wrap-restful-params :formats [:json-kw])
                 (wrap-restful-response :charset "UTF-8" :formats [:json])
                 (wrap-content-type)))

;;loop to setup
(defn- start-event-consumer!
 "consum install queue"
 []
 (start-thread!
     (fn [] ;;consume queue
       (when-let [ev (.take event-queue)]
         ;; extract queue and pids from :radarly and dissoc :radarly data
         (set-el-state! ev)))
     "cluster install consumer"))

(defn shutdown
  [shut-list]
  (doseq [shut shut-list]
      ((:stop shut))))

(defn -main
  "I don't do a whole lot ... yet."
  [path port host]
  (reset! state-path path)
  (when (.exists (io/as-file path))
    (let [state (yaml-file->map path)]
      (reset! cluster-state state)))
  (defonce server (run-jetty #'handler {:port port :host host :join? false}))
  (let [shut-list [start-event-consumer!]]
                (.addShutdownHook (Runtime/getRuntime)
                    (proxy [Thread] []
                      (run []
                        (log/info "Exit...")
                        (try
                          (shutdown shut-list)
                          (catch Exception ex
                            (log/error ex "error while exiting"))))))))
