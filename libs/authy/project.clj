(defproject org.iplantc.core/authy "0.1.0-SNAPSHOT"
  :description "OAuth client library for the Discovery Environment."
  :url "https://github.com/iPlantCollaborativeOpenSource/DiscoveryEnvironmentShared/"
  :license {:name "BSD Standard License"
            :url "http://www.iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [cheshire "5.3.1"]
                 [clj-http "0.9.1"]
                 [com.cemerick/url "0.1.1"]
                 [medley "0.1.5"]]
  :aot [authy.core])
