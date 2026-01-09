(ns shared.state
  "Shared state definition for both browser and worker"
  (:require [malli.core :as m]
            [malli.transform :as mt]
            [malli.error :as me]
            [shared.github :as gh]))

;; =============================================================================
;; State Schema
;; =============================================================================

(def State
  "Schema for application state"
  [:map
   ;; Current language
   [:current-lang {:optional true} [:enum :zh :en]]
   ;; Detailed post being viewed (nil if not viewing)
   [:visiting-post {:optional true} [:maybe gh/PostDetailed]]
   ;; Tag being viewed (nil if not viewing)
   [:visiting-tag {:optional true} [:maybe :string]]
   ;; Map of post-id -> post
   [:posts {:optional true} [:maybe [:map-of gh/PostId gh/Post]]]
   ;; List of posts for current tag
   [:tag-posts {:optional true} [:maybe [:sequential gh/Post]]]
   ;; Loading posts list?
   [:loading-post-list {:optional true} [:maybe :boolean]]
   ;; Loading tag posts?
   [:loading-tag-post-list {:optional true} [:maybe :boolean]]
   ;; Map of post-id -> loading?
   [:loading-posts {:optional true} [:maybe [:map-of gh/PostId :boolean]]]
   ;; Map of post-id -> visited?
   [:visited-posts {:optional true} [:maybe [:map-of gh/PostId :boolean]]]])

;; =============================================================================
;; JSON Serialization
;; =============================================================================

(def ^:private json-transformer
  "Transformer for JSON decode: converts strings to keywords where needed"
  (mt/json-transformer))

(defn state->js
  "Convert state to JS object for JSON serialization.
   Only includes fields necessary for SSR hydration."
  [state]
  (when-let [errors (m/explain State state)]
    (throw (ex-info "state->js: invalid state"
                    {}
                    (clj->js {:errors errors
                              :message (-> errors
                                           (me/humanize)
                                           (println-str))}))))
  (clj->js (select-keys state [:current-lang :visiting-post :visiting-tag :posts :tag-posts])))

(defn js->state
  "Convert JS object back to state map.
   Uses malli transformer to automatically convert strings to keywords."
  [js-data]
  (let [data (js->clj js-data :keywordize-keys true)
        result (m/decode State data json-transformer)]
    (when-let [errors (m/explain State result)]
      (throw (ex-info "js->state: invalid result"
                      {}
                      (clj->js {:errors errors
                                :message (-> errors
                                             (me/humanize)
                                             (println-str))}))))
    result))
