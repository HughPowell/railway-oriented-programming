(defproject railway-oriented-programming "0.1.1-SNAPSHOT"
  :description "An implementation of railway oriented programming"
  :url "https://github.com/HughPowell/railway-oriented-programming"
  :license {:name "Mozilla Public License v2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]]
  :plugins [[update-readme "0.1.0"]]
  :repositories [["releases" {:url "https://clojars.org/repo/"
                              :username :env
                              :password :env
                              :sign-releases false}]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}}
  :monkeypatch-clojure-test false
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["update-readme"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["update-readme"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
