(ns browser.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [cljs.core.async :as a]
            [browser.utils :refer [dom-ready]]
            [browser.flux :as f]
            [browser.github :as g]
            [redux-map-action.core :as rc]))

;; state

(defrecord State
    [loading-posts
     posts])

(defonce state
  (r/atom (map->State {})))


(defmulti reducer #(:type %2))

(defmethod reducer :posts-fetch [state action]
  (assoc state :loading-posts true))

(defmethod reducer :posts-fetched [state action]
  (-> state
      (assoc :loading-posts false)
      (assoc :posts (:payload action))))

(defmethod reducer :default [])


(defmulti subscribe #(:type %))

(defmethod subscribe :posts-fetch [action store]
  (go (f/dispatch! store {:type :posts-fetched
                          :payload (a/<! (g/get-posts))})))

(defmethod subscribe :default [])


;; components

(defn blog-post [post]
  ^{:key (:url post)}
  [:li (:name post)])

(defn blog-posts []
  (if (:loading-posts @state)
    [:h1 "Loading..."]
    [:ul (map blog-post (:posts @state))]))


;; initialize

(defn create-store []
  (let [devtools-enhancer (if js/window.__REDUX_DEVTOOLS_EXTENSION__
                            (js/window.__REDUX_DEVTOOLS_EXTENSION__
                             #js {:serialize
                                  #js {:replacer #(if (coll? %2)
                                                    #js {:cljs-struct true
                                                         :data (clj->js %2)}
                                                    %2)}})
                            identity)
        enhancer (comp
                  rc/enhancer
                  f/clj-atom-state-compatible-enhancer
                  (f/apply-middleware (f/chan-middleware subscribe))
                  (rc/wrap-redux-devtools-enhancer devtools-enhancer))
        store (f/create-store reducer state enhancer)]
    (f/dispatch! store {:type :posts-fetch})
    store))

(dom-ready
 (fn []
   (create-store)
   (r/render
    [blog-posts]
    (js/document.getElementById "app"))))
