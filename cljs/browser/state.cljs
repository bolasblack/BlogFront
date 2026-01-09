(ns browser.state
  (:require
   [reagent.core :as r]))

;; =============================================================================
;; Reducers
;; =============================================================================

(defmulti reducer #(:type %2))

;; Post reducers
(defmethod reducer :posts-fetch [state _]
  (assoc state :loading-post-list true))

(defmethod reducer :posts-fetched [state {:keys [posts]}]
  (let [ziped-posts (zipmap (map :id posts) posts)]
    (-> state
        (assoc :loading-post-list false)
        (assoc :posts ziped-posts))))

(defmethod reducer :post-fetch [state {:keys [post]}]
  (assoc-in state [:loading-posts (:id post)] true))

(defmethod reducer :post-fetched [state {:keys [post]}]
  (let [post-id (:id post)
        post-with-content (assoc post :content (or (:content post) ""))]
    (cond-> state
      true (assoc-in [:posts post-id] post)
      true (assoc-in [:loading-posts post-id] false)
      (= (:id (:visiting-post state)) post-id) (assoc :visiting-post post-with-content))))

(defmethod reducer :post-show [state {:keys [post]}]
  (let [post-with-content (assoc post :content (or (:content post) ""))]
    (-> state
        (assoc-in [:visited-posts (:id post)] true)
        (assoc :visiting-post post-with-content))))

(defmethod reducer :post-unshow [state {:keys [post-id]}]
  (if (= (:id (:visiting-post state)) post-id)
    (assoc state :visiting-post nil)
    state))

;; Tag reducers
(defmethod reducer :tag-show [state {:keys [tag]}]
  (-> state
      (assoc :visiting-tag tag)
      (assoc :loading-tag-post-list true)))

(defmethod reducer :tag-fetched [state {:keys [posts]}]
  (-> state
      (assoc :tag-posts posts)
      (assoc :loading-tag-post-list false)))

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
  (r/atom {}))
