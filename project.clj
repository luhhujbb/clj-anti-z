(defproject clj-anti-zoo "0.1.6"
  :description "Small state API"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ;; logging stuff
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-api "1.7.19"]
                 [org.slf4j/slf4j-log4j12 "1.7.19"]
                 [org.slf4j/jcl-over-slf4j "1.7.19"]
                 [log4j "1.2.17"]
                 [clj-time "0.11.0"]
                 ;; web service
                 [compojure "1.5.0"]
                 [ring "1.4.0"]
                 [ring-middleware-format "0.7.0"]
                 ;;for proxy
                 [clj-http "2.1.0"]
                 [cheshire "5.5.0"]
                 [clj-yaml "0.4.0"]
                 [digest "1.4.4"]]
  :main ^:skip-aot clj-anti-zoo.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
