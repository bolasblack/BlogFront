(ns browser.utils
  (:require [clojure.string :as s]
            [goog.object :as gobj]
            ["highlight.js" :as hl]
            ["markdown-it" :as mdit-generator]
            ["markdown-it-anchor" :as mdit-anchor]
            ["markdown-it-footnote" :as mdit-footnote]))

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

(defn render-md [content & {:keys [heading-id-renderer]
                            :or {heading-id-renderer #(js/encodeURIComponent %)}}]
  (let [mdit (-> (mdit-generator #js {:html true
                                      :highlight (fn [code lang]
                                                   (try
                                                     (.-value (hl/highlight lang (s/trim code)))
                                                     (catch js/Error err
                                                       (s/trim code))))})
                 (.use mdit-anchor #js {:permalink true
                                        :permalinkSymbol ""
                                        :permalinkBefore true
                                        :permalinkHref #(str "#" (heading-id-renderer (js/decodeURIComponent %)))
                                        :callback (fn [token ctx]
                                                    (->> (.-slug ctx)
                                                         (js/decodeURIComponent)
                                                         (heading-id-renderer)
                                                         (.attrSet token "id")))})
                 (.use mdit-footnote))
        rules (.. mdit -renderer -rules)
        get-refid (fn [tokens idx options env slf]
                    (let [id (.. slf -rules (footnote_anchor_name tokens idx options env slf))
                          subid (gobj/getValueByKeys tokens idx "meta" "subId")
                          refid (if (> subid 0) (str id ":" subid) id)]
                      refid))]
    (set! (.-footnote_anchor rules)
          (fn [tokens idx options env slf]
            (let [refid (get-refid tokens idx options env slf)]
              (str "<a "
                   "href='#" (heading-id-renderer (str "fnref" refid))"' "
                   "class='footnote-backref'>\u21a9\uFE0E</a>"))))
    (set! (.-footnote_open rules)
          (fn [tokens idx options env slf]
            (let [refid (get-refid tokens idx options env slf)]
              (str "<li id='" (heading-id-renderer (str "fn" refid)) "' "
                   "class='footnote-item'>"))))
    (set! (.-footnote_ref rules)
          (fn [tokens idx options env slf]
            (let [refid (get-refid tokens idx options env slf)
                  caption (.. slf -rules (footnote_caption tokens idx options env slf))]
              (str "<sup class='footnote-ref'>"
                   "<a href='#" (heading-id-renderer (str "fn" refid))"' "
                   "id=" (heading-id-renderer (str "fnref" refid)) " "
                   ">"
                   caption
                   "</a></sup>"))))
    (.render mdit content)))

(defmacro js-swap! [var-symbol expr]
  `(set! ,var-symbol (,expr ,var-symbol)))
