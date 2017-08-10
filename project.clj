(defproject graph-clj-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.walmartlabs/lacinia "0.20.0"]                 
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [org.clojure/core.async "0.3.443"] 
                 [com.contentful.java/java-sdk "7.6.2"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler graph-clj-test.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})