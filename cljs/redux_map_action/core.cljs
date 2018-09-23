(ns redux-map-action.core)

(defn serialize-action [action]
  (if (coll? action)
    #js {:type (str (:type action))
         :action action}
    action))

(defn deserialize-action [action]
  (if (coll? action.action)
    action.action
    action))

(defn- wrap-redux-store-dispatch [redux-store]
  (js/Object.assign
   #js {}
   redux-store
   #js {:dispatch #(.dispatch redux-store (serialize-action %1))}))

(defn enhancer [create-store]
  (fn [reducer preloaded-state enhancer]
    (let [compatible-reducer #(reducer %1 (deserialize-action %2))
          redux-store (create-store compatible-reducer preloaded-state enhancer)]
      (wrap-redux-store-dispatch redux-store))))

(defn wrap-redux-devtools-enhancer [devtools-enhancer]
  (fn [create-store]
    (fn [reducer preloaded-state enhancer]
      (let [redux-store ((devtools-enhancer create-store) reducer preloaded-state enhancer)]
        (wrap-redux-store-dispatch redux-store)))))
