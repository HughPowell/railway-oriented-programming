(ns railway-oriented-programming.core-test
  (:refer-clojure :exclude [map])
  (:require [clojure.test :refer :all]
            [railway-oriented-programming.core :refer :all]
            [clojure.string :as s]))

(def blank-name-failure (fail "Name must not be blank"))
(defn validate-1 [{:keys [name] :as input}] (if (s/blank? name) blank-name-failure (succeed input)))

(def too-long-name-failure (fail "Name must not be longer than 50 characters"))
(defn validate-2 [{:keys [name] :as input}] (if (> (count name) 50) too-long-name-failure (succeed input)))

(def blank-email-failure (fail "Email must not be blank"))
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
           (with-out-str (log-failure (second blank-name-failure)))))))
