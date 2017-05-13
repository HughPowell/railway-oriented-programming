(ns railway-oriented-programming.core-test
  (:refer-clojure :exclude [map])
  (:require [clojure.test :refer :all]
            [railway-oriented-programming.core :refer :all]
            [clojure.string :as s]))

(def blank-name-failure-text "Name must not be blank")
(def blank-name-failure (fail blank-name-failure-text))
(defn validate-1 [{:keys [name] :as input}] (if (s/blank? name) blank-name-failure (succeed input)))

(def too-long-name-failure-text "Name must not be longer than 50 characters")
(def too-long-name-failure (fail too-long-name-failure-text))
(defn validate-2 [{:keys [name] :as input}] (if (> (count name) 50) too-long-name-failure (succeed input)))

(def blank-email-failure-text "Email must not be blank")
(def blank-email-failure (fail blank-email-failure-text))
(defn validate-3 [{:keys [email] :as input}] (if (s/blank? email) blank-email-failure (succeed input)))

(defn- assert-combined-validation [combined-validation]
  (is (= (combined-validation {:name "" :email ""}) blank-name-failure))
  (is (= (combined-validation {:name "Alice" :email ""}) blank-email-failure))
  (is (= (combined-validation {:name "Alice" :email "good"}) (succeed {:name "Alice" :email "good"}))))

(deftest composing-bound-functions
  (assert-combined-validation (>> validate-1
                                  (bind validate-2)
                                  (bind validate-3))))

(deftest composing-functions-bound-inline
  (assert-combined-validation (>>= validate-1
                                   validate-2
                                   validate-3)))

(deftest switch-composition
  (assert-combined-validation (>=> validate-1
                                   validate-2
                                   validate-3)))

(defn- canonicalise-email [{:keys [email] :as input}]
  (assoc input :email (s/lower-case (s/trim email))))

(defn- assert-usecase [usecase]
  (is (= (usecase {:name "Alice" :email "UPPERCASE     "}) (succeed {:name "Alice" :email "uppercase"})))
  (is (= (usecase {:name "" :email "UPPERCASE     "}) blank-name-failure)))

(deftest switch-fn
  (assert-usecase (>=> validate-1
                       validate-2
                       validate-3
                       (switch canonicalise-email))))

(deftest map-fn
  (assert-usecase (>>
                    (>>= validate-1
                         validate-2
                         validate-3)
                    (map canonicalise-email))))

(defn- update-database [])

(deftest tee-fn
  (assert-usecase (>=> validate-1
                       validate-2
                       validate-3
                       (switch canonicalise-email)
                       (switch (tee update-database)))))

(deftest tee-fn-two-track-style
  (assert-usecase (>> validate-1
                      (bind validate-2)
                      (bind validate-3)
                      (map canonicalise-email)
                      (map (tee update-database)))))

(deftest try-catch-fn
  (assert-usecase (>=> validate-1
                       validate-2
                       validate-3
                       (switch canonicalise-email)
                       (try-catch (tee update-database)))))

(defn- log-success [x]
  (prn "DEBUG. Success so far:" x)
  x)

(defn- log-failure [x]
  (prn "ERROR." x)
  x)

(defn- log [two-track-input]
  ((double-map log-success log-failure) two-track-input))

(deftest double-map-fn
  (let [usecase (>>
                  (>=> validate-1
                       validate-2
                       validate-3
                       (switch canonicalise-email)
                       (try-catch (tee update-database)))
                  log)
        good-input {:name "Alice" :email "good"}
        bad-input {:name "" :email ""}]
    (is (= (with-out-str (usecase good-input))
           (with-out-str (log-success good-input))))
    (is (= (with-out-str (usecase bad-input))
           (with-out-str (log-failure blank-name-failure-text))))))

(defn- add-success [& rs] (first rs))
(defn- add-failure [& ss] (s/join "; " ss))

(defn- &&& [& vs]
  (apply plus add-success add-failure vs))

(deftest plus-fn
  (let [combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)]
    (is (= (combined-validation {:name "" :email ""})
           (fail (add-failure blank-name-failure-text
                              blank-email-failure-text))))
    (is (= (combined-validation {:name "Alice" :email ""})
           (fail (add-failure blank-email-failure-text))))
    (let [good-input {:name "Alice" :email "good"}]
      (is (= (combined-validation good-input)
             (succeed (add-success good-input)))))))

(deftest re-define-usecase
  (let [combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)
        usecase (>=> combined-validation
                     (switch canonicalise-email)
                     (tee update-database))]
    (is (= (usecase {:name "Alice" :email "UPPERCASE    "})
           {:name "Alice" :email "uppercase"}))))

(deftest dynamic-injection
  (let [success (fn [x] (println "DEBUG. Success so far " x) x)
        debug-logger (fn [two-track-input]
                       (letfn [(failure [x] (identity x))]
                         ((double-map success failure) two-track-input)))
        injectable-logger (fn [config] (if (:debug config) debug-logger identity))
        combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)
        usecase (fn [config] (>> combined-validation
                                 (map canonicalise-email)
                                 (injectable-logger config)))
        input {:name "Alice" :email "good"}]
    (is (= (with-out-str ((usecase {:debug false}) input))
           "")
        (= (with-out-str ((usecase {:debug true}) input))
           (with-out-str (success input))))))
