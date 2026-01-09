(ns browser.utils
  (:require [clojure.string :as str]
            [shared.markdown :as md]))

(defn classnames [& cs]
  (->> cs
       (map #(cond
               (map? %) (apply classnames %)
               (coll? %) (if (last %) (name (first %)) nil)
               :else (name %)))
       (str/join " ")))

(defn dom-ready [callback]
  ;; https://github.com/jquery/jquery/blob/master/src/core/ready.js
  ;; https://developer.mozilla.org/en-US/docs/Web/API/Document/readyState
  (if (not= js/document.readyState "loading")
    (callback)
    (let [callback-when-loaded (fn callback-when-loaded []
                                 (js/document.removeEventListener "DOMContentLoaded" callback-when-loaded)
                                 (callback))]
      (js/document.addEventListener "DOMContentLoaded" callback-when-loaded))))

(defn render-md
  "Render markdown for browser"
  [content & {:keys [heading-id-renderer lang]
              :or {heading-id-renderer #(js/encodeURIComponent %)}}]
  (when content
    (md/render-md content
                  :heading-id-renderer heading-id-renderer
                  :lang lang)))

(defn scroll-to-element-by-id
  "Scroll to element by its ID"
  [elem-id]
  (when-let [elem (js/document.getElementById elem-id)]
    (let [elem-rect (.getBoundingClientRect elem)
          scroll-top (+ js/document.scrollingElement.scrollTop elem-rect.top)]
      (js/scrollTo #js {:top scroll-top
                        :behavior "smooth"}))))