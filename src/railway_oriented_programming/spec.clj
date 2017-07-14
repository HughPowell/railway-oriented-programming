(ns railway-oriented-programming.spec
  (:require [clojure.spec.alpha :as spec]
            [railway-oriented-programming.core :as rop]))

(spec/def ::rop/value some?)
(spec/def ::rop/error some?)

(spec/def ::rop/success (spec/tuple some? nil?))
(spec/def ::rop/failure (spec/tuple nil? some?))

(spec/def ::rop/result (spec/or :success ::rop/success
                                :failure ::rop/failure))

(spec/fdef rop/succeeded?
           :args (spec/cat :result ::rop/result)
           :ret boolean?)

(spec/fdef rop/failed?
           :args (spec/cat :result ::rop/result)
           :ret boolean?)

(spec/fdef rop/succeed
           :args (spec/cat :value ::rop/value)
           :ret ::rop/success)

(spec/fdef rop/fail
           :args (spec/cat :error ::rop/error)
           :ret ::rop/failure)

(spec/fdef rop/success
           :args (spec/cat :success ::rop/success)
           :ret ::rop/value)

(spec/fdef rop/failure
           :args (spec/cat :failure ::rop/failure)
           :ret ::rop/error)
