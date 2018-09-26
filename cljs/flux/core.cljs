(ns flux.core
  (:require [redux-map-action.core :as rc]
            ["redux" :as r]))

(defprotocol IStore
  (dispatch! [this action]
    "(dispatch! store {:type :action-type :payload {:other \"payload\"}})")
  (subscribe [this fn]
    "(subscribe store (fn [] (dispatch! store {:type :action})))")
  (get-state [this]
    "(get-state store)")
  (replace-reducer! [this reducer]
    "(replace-reducer store reducer)"))

(defrecord Store [redux-store]
  IStore
  (dispatch! [this action]
    (.dispatch (:redux-store this) action))
  (subscribe [this f]
    (.subscribe (:redux-store this) #(f)))
  (get-state [this]
    (.getState (:redux-store this)))
  (replace-reducer! [this reducer]
    (.replaceReducer (:redux-store this) reducer)))

(defn create-store
  "(create-store reducer preloaded-state? enhancer?)"
  ([reducer]
   (create-store reducer nil nil))
  ([reducer preloaded-state]
   (create-store reducer preloaded-state nil))
  ([reducer preloaded-state enhancer]
   (let [compatible-reducer #(reducer %1 %2)
         redux-store (r/createStore compatible-reducer preloaded-state enhancer)]
     (Store. redux-store))))


(defn apply-middleware [& middlewares]
  (let [compatible-middlewares (map (fn [f] #(apply f %&)) middlewares)]
    (apply r/applyMiddleware compatible-middlewares)))


(defn clj-atom-state-compatible-enhancer [create-store]
  (fn [reducer preloaded-state enhancer]
    (if-not (satisfies? IAtom preloaded-state)
      (create-store reducer preloaded-state enhancer)
      (let [redux-store (create-store reducer @preloaded-state enhancer)]
        (.subscribe redux-store #(reset! preloaded-state (.getState redux-store)))
        redux-store))))


(defn chan-middleware [subscriber & {:keys [dependencies]
                                     :or {dependencies {}}}]
  (fn [redux-store]
    (fn [next-dispatch]
      (fn [action]
        (let [result (next-dispatch action)]
          (subscriber
           (rc/deserialize-action action)
           (Store. redux-store)
           dependencies)
          result)))))
