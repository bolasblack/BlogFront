(ns browser.router
  "Browser-specific navigation utilities using History API"
  (:require [shared.router :as r]))

;; =============================================================================
;; Navigation History
;; =============================================================================

(def ^:private NAV_DEPTH_KEY "blog-nav-depth")
(def ^:private CURRENT_DEPTH_KEY "blog-current-depth")

(defn get-nav-depth
  "Get navigation depth from history state"
  []
  (or (when js/history.state (aget js/history.state NAV_DEPTH_KEY)) 0))

(defn update-nav-depth!
  "Update navigation depth in session storage and history state"
  []
  (if (and js/history.state (aget js/history.state NAV_DEPTH_KEY))
    ;; Back navigation or refresh - restore from history state
    (js/sessionStorage.setItem CURRENT_DEPTH_KEY
                               (aget js/history.state NAV_DEPTH_KEY))
    ;; Forward navigation - increment and save
    (let [current-depth (js/parseInt
                         (or (js/sessionStorage.getItem CURRENT_DEPTH_KEY) "0")
                         10)
          new-depth (inc current-depth)]
      (js/sessionStorage.setItem CURRENT_DEPTH_KEY new-depth)
      (js/history.replaceState (js-obj NAV_DEPTH_KEY new-depth) ""))))

(defn navigate!
  "Navigate to a new URL using History API"
  [url]
  (js/history.pushState #js {} "" url)
  ;; Dispatch popstate event for the router to handle
  (js/window.dispatchEvent (js/PopStateEvent. "popstate")))

(defn go-back!
  "Navigate back or to home if at the beginning of history"
  [e lang]
  (.preventDefault e)
  (if (> (get-nav-depth) 1)
    (js/history.back)
    (navigate! (r/home-url lang))))
