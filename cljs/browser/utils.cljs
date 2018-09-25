(ns browser.utils
  (:require [clojure.string :as s]
            ["marked" :as md]
            ["highlight.js" :as hl]))

(defn classnames [& cs]
  (->> cs
       (map #(cond
               (map? %) (apply classnames %)
               (coll? %) (if (last %) (name (first %)) nil)
               :else (name %)))
       (s/join " ")))


(defn dom-ready [callback]
  ;; https://github.com/jquery/jquery/blob/master/src/core/ready.js
  ;; https://developer.mozilla.org/en-US/docs/Web/API/Document/readyState
  (if (not= js/document.readyState "loading")
    (callback)
    (let [callback-when-loaded (fn callback-when-loaded []
                                 (js/document.removeEventListener "DOMContentLoaded" callback-when-loaded)
                                 (callback))]
      (js/document.addEventListener "DOMContentLoaded" callback-when-loaded))))

(defn render-md [content]
  (let [renderer (md/Renderer.)
        _ (set!
           renderer.heading
           (fn [text level raw]
             (str "<h" level " id=\"" (js/encodeURIComponent raw) "\">" text "</h1>")))]
    (md content #js {:renderer renderer
                     :highlight (fn [code lang]
                                  (try
                                    (.-value (hl/highlight lang code))
                                    (catch js/Error err
                                      code)))})))