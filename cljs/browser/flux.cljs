(ns browser.flux
  (:refer-clojure :exclude [reduce])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]))

(defonce actions (a/chan))

(defn dispatch
  "Dispatch new action. Type should be keyword."
  ([type] (dispatch type nil))
  ([type payload]
   (pr "dispatch" type payload)
   (a/put! actions {:type type :payload payload})))

(defmulti reduce
  (fn [state payload action-type] action-type))

(defmulti subscribe
  (fn [state-ref action dispatch] (:type action)))

(defmethod subscribe :default [])

(defn init [state-ref]
  (go-loop []
    (when-let [action (a/<! actions)]
      (let [{:keys [type payload]} action]
        (println "Handle action" type)
        (swap! state-ref reduce payload type)
        (subscribe state-ref action dispatch))
      (recur))))
