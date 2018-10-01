(ns redux.chan-middleware
  (:require
   [cljs.core.async :as a :include-macros true]
   [utils.async :as ua :include-macros true]
   [redux.core :refer [Store]]
   [redux-map-action.core :as rc]))

(defn next-action [action-chan type]
  (ua/go-try
   (loop []
     (let [action (a/<! action-chan)]
       (if (= type (:type action))
         action
         (recur))))))

(defn chan-middleware [subscriber & {:keys [dependencies]
                                     :or {dependencies {}}}]
  (fn [redux-store]
    (let [action-chan (a/chan)
          result-chan (a/chan)
          subscriber-chan (subscriber action-chan result-chan dependencies)]

      (a/go-loop []
        (when-let [action (a/<! result-chan)]
          (.dispatch redux-store action))
        (recur))

      (fn [next-dispatch]
        (fn [action]
          (let [result (next-dispatch action)]
            (a/put! action-chan action)
            result))))))
