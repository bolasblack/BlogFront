(ns browser.state
  (:require
   [reagent.core :as r]
   [browser.github :as g]))

;; =============================================================================
;; State Definition
;; =============================================================================

(defrecord State
    [loading-post-list
     posts
     loading-posts
     visited-posts
     visiting-post
     ;; Tag view state
     visiting-tag
     tag-posts
     loading-tag
     ;; Language state
     current-lang])

;; =============================================================================
;; Reducers
;; =============================================================================

(defmulti reducer #(:type %2))

;; Post reducers
(defmethod reducer :posts-fetch [state action]
  (assoc state :loading-post-list true))

(defmethod reducer :posts-fetched [state {:keys [posts]}]
  (let [ziped-posts (zipmap (map g/id posts) posts)]
    (-> state
        (assoc :loading-post-list false)
        (assoc :posts ziped-posts))))

(defmethod reducer :post-fetch [state {:keys [post]}]
  (assoc-in state [:loading-posts (g/id post)] true))

(defmethod reducer :post-fetched [state {:keys [post]}]
  (-> state
      (assoc-in [:posts (g/id post)] post)
      (assoc-in [:loading-posts (g/id post)] false)))

(defmethod reducer :post-show [state {:keys [post]}]
  (-> state
      (assoc-in [:visited-posts (g/id post)] true)
      (assoc-in [:visiting-post] (g/id post))))

(defmethod reducer :post-unshow [state {:keys [post-id]}]
  (if (= (:visiting-post state) post-id)
    (assoc state :visiting-post nil)
    state))

;; Tag reducers
(defmethod reducer :tag-show [state {:keys [tag]}]
  (-> state
      (assoc :visiting-tag tag)
      (assoc :loading-tag true)))

(defmethod reducer :tag-fetched [state {:keys [tag posts]}]
  (-> state
      (assoc :tag-posts posts)
      (assoc :loading-tag false)))

(defmethod reducer :tag-unshow [state _]
  (-> state
      (assoc :visiting-tag nil)
      (assoc :tag-posts nil)))

;; Language reducer
(defmethod reducer :lang-changed [state {:keys [lang]}]
  (assoc state :current-lang lang))

;; Default
(defmethod reducer :default [state] state)

;; =============================================================================
;; State
;; =============================================================================

(defonce state
  (r/atom (map->State {})))
