(ns browser.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as r]
            [cljs.core.async :as a]
            [browser.utils :refer [dom-ready]]
            [browser.flux :as f]
            [browser.github :as g]))

;; state

(defrecord State
    [loading-posts
     posts])

(defonce state
  (r/atom (map->State {})))

(defmethod f/reduce :posts-fetch [state]
  (assoc state :loading-posts true))

(defmethod f/reduce :posts-fetched [state posts]
  (-> state
      (assoc :loading-posts false)
      (assoc :posts posts)))

(defmethod f/subscribe :posts-fetch
  [state-ref action dispatch]
  (go (dispatch :posts-fetched (a/<! (g/get-posts)))))


;; components

(defn blog-post [post]
  ^{:key (:url post)}
  [:li (:name post)])

(defn blog-posts []
  (if (:loading-posts @state)
    [:h1 "Loading..."]
    [:ul (map #(vector blog-post %) (:posts @state))]))


;; initialize

(dom-ready
 (fn []
   (f/init state)
   (f/dispatch :posts-fetch)
   (r/render
    [blog-posts]
    (js/document.getElementById "app"))))
