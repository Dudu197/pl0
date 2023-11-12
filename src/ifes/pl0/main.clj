(ns ifes.pl0.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [ifes.pl0.core :as pl0])
  (:gen-class))


(defn -main
  [& args]
  (pl0/exec (slurp (nth args 0))))

