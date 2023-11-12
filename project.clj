(defproject pl0 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "GPL-3.0-or-later WITH Classpath-exception-2.0"
            :url "https://spdx.org/licenses/GPL-3.0-or-later.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.match "1.0.1"]
                 [org.clojure/tools.cli "1.0.219"]
                 [instaparse "1.4.12"]]
  :main ^:skip-aot ifes.pl0.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
