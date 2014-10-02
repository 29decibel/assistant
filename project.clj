(defproject assistant "0.1.0-SNAPSHOT"
  :description "A super awesome extensible personal assistant"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [cljs-http "0.1.15"]
                 [com.cognitect/transit-cljs "0.8.188"]
                 [markdown-clj "0.9.54"]
                 [domina "1.0.2"]
                 [garden "1.2.1"]
                 [prismatic/dommy "0.1.3"]
                 [hickory "0.5.4"]
                 [om "0.7.0"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [lein-node-webkit-build "0.1.0"]]

  :source-paths ["src"]

  :node-webkit-build {:root "./public"
                      :name "Assistant"
                      :osx {:icon "./public/images/icon.icns"}
                      :platforms #{:osx}
                      :disable-developer-toolbar true}

  :cljsbuild {
    :builds [{:id "assistant"
              :source-paths ["src" "plugins"]
              :compiler {
                :output-to "public/assistant.js"
                :output-dir "public/out"
                :optimizations :none
                :source-map true}}]})
