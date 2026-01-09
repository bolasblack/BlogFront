(ns redux.chan-middleware
  (:require
   [cljs.core.async :as a :include-macros true]
   [rxcljs.core :as rc :include-macros true]))

(defn next-action [action-chan type]
  (rc/go-loop []
    (let [action (rc/<! action-chan)]
      (if (= type (:type action))
        action
        (recur)))))

(defn chan-middleware [subscriber & {:keys [dependencies]
                                     :or {dependencies {}}}]
  (fn [redux-store]
    (let [action-chan (a/chan)
          result-chan (a/chan)]
      ;; Start subscriber (return value not needed)
      (subscriber action-chan result-chan dependencies)

      (rc/go-loop []
        (when-let [action (rc/<! result-chan)]
          (.dispatch ^js redux-store action))
        (recur))

      (fn [next-dispatch]
        (fn [action]
          (let [result (next-dispatch action)]
            (a/put! action-chan action)
            result))))))
