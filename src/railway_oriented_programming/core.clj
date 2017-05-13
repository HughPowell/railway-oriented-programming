(ns railway-oriented-programming.core
  (:refer-clojure :rename {map core-map}))

(defn- execute [[value :as two-track-input] switch-fn]
  (if value
    (switch-fn value)
    two-track-input))

(defn succeed [value]
  [value nil])

(defn fail [error]
  [nil error])

(defn bind [switch-fn]
  (fn [two-track-input]
    (execute two-track-input switch-fn)))

(defn >> [& switch-fns]
  (apply comp (reverse switch-fns)))

(defn >>= [switch-fn & switch-fns]
  (apply >> (cons switch-fn (core-map bind switch-fns))))

(defn >=> [& fns]
  (letfn [(merge [switch-fn-1 switch-fn-2]
            (fn [input] (execute (switch-fn-1 input) switch-fn-2)))]
    (reduce merge fns)))

(defn switch [one-track-fn]
  (fn [one-track-input]
    (succeed (one-track-fn one-track-input))))

(defn map [one-track-fn]
  (fn [value]
    (execute value (comp succeed one-track-fn))))

(defn tee [dead-end-fn]
  (fn [input]
    (dead-end-fn)
    input))

(defn try-catch [one-track-fn]
  (fn [one-track-input]
    (try
      (succeed (one-track-fn one-track-input))
      (catch Exception e (fail e)))))

(defn double-map [success-fn failure-fn]
  (fn [[value error]]
    (if value
      (succeed (success-fn value))
      (fail (failure-fn error)))))