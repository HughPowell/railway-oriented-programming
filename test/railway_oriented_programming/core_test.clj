(ns railway-oriented-programming.core-test
  "Copyright (c) 2017.
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/."
  (:require [clojure.test :refer :all]
            [railway-oriented-programming.spec :as rop-spec]
            [clojure.spec.test.alpha :as spec-test]
            [railway-oriented-programming.core :as rop]
            [clojure.string :as string]))

(spec-test/instrument)

(def blank-name-failure-text "Name must not be blank")
(defn validate-1 [{:keys [name] :as input}] (if (string/blank? name)
                                              (rop/fail blank-name-failure-text)
                                              (rop/succeed input)))

(def too-long-name-failure-text "Name must not be longer than 50 characters")
(defn validate-2 [{:keys [name] :as input}] (if (> (count name) 50)
                                              (rop/fail too-long-name-failure-text)
                                              (rop/succeed input)))

(def blank-email-failure-text "Email must not be blank")
(defn validate-3 [{:keys [email] :as input}] (if (string/blank? email)
                                               (rop/fail blank-email-failure-text)
                                               (rop/succeed input)))

(defn- assert-combined-validation [combined-validation]
  (is (= (rop/failure (combined-validation {:name "" :email ""}))
         blank-name-failure-text))
  (is (= (rop/failure (combined-validation {:name "Alice" :email ""}))
         blank-email-failure-text))
  (is (= (rop/success (combined-validation {:name "Alice" :email "good"}))
         {:name "Alice" :email "good"})))

(deftest something
  (rop/bind validate-1))

(deftest composing-bound-functions
  (assert-combined-validation (rop/>> validate-1
                                      (rop/bind validate-2)
                                      (rop/bind validate-3))))

(deftest composing-functions-bound-inline
  (assert-combined-validation (rop/>>= validate-1
                                       validate-2
                                       validate-3)))

(deftest switch-composition
  (assert-combined-validation (rop/>=> validate-1
                                       validate-2
                                       validate-3)))

(defn- canonicalise-email [{:keys [email] :as input}]
  (assoc input :email (string/lower-case (string/trim email))))

(defn- assert-usecase [usecase]
  (is (= (rop/success (usecase {:name "Alice" :email "UPPERCASE     "})) {:name "Alice" :email "uppercase"}))
  (is (= (rop/failure (usecase {:name "" :email "UPPERCASE     "})) blank-name-failure-text)))

(deftest switch-fn
  (assert-usecase (rop/>=> validate-1
                           validate-2
                           validate-3
                           (rop/switch canonicalise-email))))

(deftest map-fn
  (assert-usecase (rop/>>
                    (rop/>>= validate-1
                             validate-2
                             validate-3)
                    (rop/map canonicalise-email))))

(defn- update-database [_])

(deftest tee-fn
  (assert-usecase (rop/>=> validate-1
                           validate-2
                           validate-3
                           (rop/switch canonicalise-email)
                           (rop/switch (rop/tee update-database)))))

(deftest tee-fn-two-track-style
  (assert-usecase (rop/>> validate-1
                          (rop/bind validate-2)
                          (rop/bind validate-3)
                          (rop/map canonicalise-email)
                          (rop/tee update-database))))

(deftest try-catch-fn
  (assert-usecase (rop/>=> validate-1
                           validate-2
                           validate-3
                           (rop/switch canonicalise-email)
                           (rop/try-catch (rop/tee update-database)))))

(defn- log-success [x]
  (prn "DEBUG. Success so far:" x)
  x)

(defn- log-failure [x]
  (prn "ERROR." x)
  x)

(defn- log [two-track-input]
  ((rop/double-map log-success log-failure) two-track-input))

(deftest double-map-fn
  (let [usecase (rop/>>
                  (rop/>=> validate-1
                           validate-2
                           validate-3
                           (rop/switch canonicalise-email)
                           (rop/try-catch (rop/tee update-database)))
                  log)
        good-input {:name "Alice" :email "good"}
        bad-input {:name "" :email ""}]
    (is (= (with-out-str (usecase good-input))
           (with-out-str (log-success good-input))))
    (is (= (with-out-str (usecase bad-input))
           (with-out-str (log-failure blank-name-failure-text))))))

(defn- add-success [& rs] (first rs))
(defn- add-failure [& ss] (string/join "; " ss))

(defn- &&& [& vs]
  (apply rop/plus add-success add-failure vs))

(deftest plus-fn
  (let [combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)]
    (is (= (rop/failure (combined-validation {:name "" :email ""}))
           (add-failure blank-name-failure-text
                        blank-email-failure-text)))
    (is (= (rop/failure (combined-validation {:name "Alice" :email ""}))
           (add-failure blank-email-failure-text)))
    (let [good-input {:name "Alice" :email "good"}]
      (is (= (rop/success (combined-validation good-input))
             (add-success good-input))))))

(deftest re-define-usecase
  (let [combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)
        usecase (rop/>=> combined-validation
                         (rop/switch canonicalise-email)
                         (rop/tee update-database))]
    (is (= (usecase {:name "Alice" :email "UPPERCASE    "})
           {:name "Alice" :email "uppercase"}))))

(deftest dynamic-injection
  (let [success (fn [x] (println "DEBUG. Success so far " x) x)
        debug-logger (fn [two-track-input]
                       (letfn [(failure [x] (identity x))]
                         ((rop/double-map success failure) two-track-input)))
        injectable-logger (fn [config] (if (:debug config) debug-logger identity))
        combined-validation (&&& validate-1
                                 validate-2
                                 validate-3)
        usecase (fn [config] (rop/>> combined-validation
                                     (rop/map canonicalise-email)
                                     (injectable-logger config)))
        input {:name "Alice" :email "good"}]
    (is (= (with-out-str ((usecase {:debug false}) input))
           ""))
    (is (= (with-out-str ((usecase {:debug true}) input))
           (with-out-str (success input))))))
