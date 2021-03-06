(ns railway-oriented-programming.core
  "Copyright (c) 2017.
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/."
  (:refer-clojure :rename {map core-map}))

(defn succeeded?
  "Determines if the two-track-input contains a success"
  [[value error]]
  (and (some? value) (nil? error)))

(defn failed?
  "Determines if the two-track-input contains a failure"
  [[value error]]
  (and (nil? value) (some? error)))

(defn succeed
  "Creates a two-track result by placing the value on the success
  branch."
  [value]
  [value nil])

(defn fail
  "Creates a two-track result by placing the error on the failure
  branch."
  [error]
  [nil error])

(defn success
  "Gets the success from the two-track-input."
  [[value _]]
  value)

(defn failure
  "Gets the failure from the two-track-input."
  [[_ error]]
  error)

(defmulti execute (fn [two-track-input _] (succeeded? two-track-input)))
(defmethod execute true [two-track-input switch-fn]
  (-> two-track-input success switch-fn))
(defmethod execute :default [two-track-input _] two-track-input)

(defn bind
  "An adapter that takes a switch function and creates a new function
  that takes a two-track value as input.  If the returned function
  is called with an arg that has a failure branch the arg is
  returned unchanged, otherwise the result of calling switch-fn on
  the arg is returned."
  [switch-fn]
  (fn [two-track-input]
    (execute two-track-input switch-fn)))

(defn >>
  "Takes a set of functions and returns a fn that is the composition
  of those fns.  The returned fn takes a variable number of args,
  applies the leftmost of fns to the args, the next
  fn (left-to-right) to the result, etc."
  [& fns]
  (apply comp (reverse fns)))

(defn >>=
  "Takes a set of switch functions, calls
  railway-oriented-programming/bind on them and returns a fn that
  is the composition of those bound fns.  The returned fn takes a
  single two-track arg and applies switch-fn to that arg.  This
  result is then applied to the leftmost switch-fns, and so on,
  through the list of switch-fns (left-to-right)."
  [switch-fn & switch-fns]
  (apply >> (cons switch-fn (core-map bind switch-fns))))

(defn >=>
  "Takes a set of switch functions and returns a fn that is the
  composition of those fns.  The returned fn takes a single
  two-track arg, applies the left-most of the switch functions
  to that arg, the next switch function (left-to-right) to the
  result.  This continues until either an error is returned from
  one of the switch-fns or the last fns is called."
  [& switch-fns]
  (letfn [(merge [switch-fn-1 switch-fn-2]
            (fn [one-track-input]
              (execute (switch-fn-1 one-track-input) switch-fn-2)))]
    (reduce merge switch-fns)))

(defn switch
  "An adapter that takes a one-track function and turns it into a
  switch function.  The returned function places the result of
  one-track-fn on the success branch."
  [one-track-fn]
  (fn [one-track-input]
    (succeed (one-track-fn one-track-input))))

(defn map
  "An adapter that takes a one-track function and turns it into a
  two-track function.  The returned function places the result of
  one-track-fn, if it is executed, on the success branch."
  [one-track-fn]
  (fn [two-track-input]
    (execute two-track-input (comp succeed one-track-fn))))

(defn tee
  "An adapter that takes a dead-end-fn and turns it into a
  function that returns its input once it has executed the
  dead-end-fn."
  [dead-end-fn]
  (fn [one-or-two-track-input]
    (dead-end-fn one-or-two-track-input)
    one-or-two-track-input))

(defn try-catch
  "An adapter that takes a one-track function and turns it into a
  switch function.  The returned function places the result of
  one-track-fn on the success branch or any thrown exception on the
  failure branch."
  [one-track-fn]
  (fn [one-track-input]
    (try
      (succeed (one-track-fn one-track-input))
      (catch Exception e (fail e)))))

(defn double-map
  "An adapter that takes two one-track functions and turns them into
  a single two-track function."
  [success-fn failure-fn]
  (fn [two-track-input]
    (if (succeeded? two-track-input)
      (-> two-track-input success success-fn succeed)
      (-> two-track-input failure failure-fn fail))))

(defn plus
  "Returns a function that executes all switch-fns in parallel and
  merges the results using merge-success if they all completed
  successfully or merges all of the failures using merge-failure
  otherwise."
  [merge-success merge-failure & switch-fns]
  (fn [one-track-input]
    (let [results ((apply juxt switch-fns) one-track-input)
          failures (filter failed? results)]
      (if (empty? failures)
        (succeed (apply merge-success (core-map success results)))
        (fail (apply merge-failure (core-map failure failures)))))))

