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

(defn- md-link->blog-url
  "Convert markdown relative link to blog URL.
   ./2024-01-01-my-post.md -> #/2024-01-01-my-post (or #/en/2024-01-01-my-post)
   Does NOT convert absolute URLs (http/https)"
  [href lang]
  (when (and href
             ;; Must end with .md
             (s/ends-with? href ".md")
             ;; Must NOT be an absolute URL
             (not (s/starts-with? href "http://"))
             (not (s/starts-with? href "https://"))
             ;; Must be a relative path (starts with ./, ../, or just filename)
             (re-matches #"^\.{0,2}/?.+\.md$" href))
    (let [;; Extract filename without path and extension
          filename (-> href
                       (s/replace #"^\.{0,2}/" "")
                       (s/replace #"\.md$" ""))
          prefix (if (= lang :en) "#/en/" "#/")]
      (str prefix (js/encodeURIComponent filename)))))

(defn render-md [content & {:keys [heading-id-renderer lang]
                            :or {heading-id-renderer #(js/encodeURIComponent %)}}]
  (let [mdit (-> (mdit-generator #js {:html true
                                      :highlight (fn [code lang]
                                                   (try
                                                     (.-value (hl/highlight (s/trim code) #js {:language lang}))
                                                     (catch js/Error err
                                                       (s/trim code))))})
                 (.use mdit-anchor
                       #js {:permalink (mdit-anchor/permalink.linkInsideHeader
                                        #js {:class "header-anchor"
                                             :symbol ""
                                             :ariaHidden true
                                             :placement "before"
                                             :renderHref #(str "#" (heading-id-renderer (js/decodeURIComponent %)))})
                            :callback (fn [token info]
                                        (->> (.-slug info)
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
    ;; Override link_open to handle external links and internal .md links
    (let [default-link-open (or (.-link_open rules)
                                (fn [tokens idx options env slf]
                                  (.renderToken slf tokens idx options)))]
      (set! (.-link_open rules)
            (fn [tokens idx options env slf]
              (let [token (aget tokens idx)
                    href-idx (.attrIndex token "href")
                    href (when (>= href-idx 0)
                           (aget (aget (.-attrs token) href-idx) 1))
                    is-external? (and href
                                      (or (s/starts-with? href "http://")
                                          (s/starts-with? href "https://")))
                    blog-url (md-link->blog-url href lang)]
                ;; Handle internal .md links
                (when blog-url
                  (.attrSet token "href" blog-url))
                ;; Handle external links
                (when is-external?
                  (.attrSet token "target" "_blank")
                  (.attrSet token "rel" "noopener")
                  (.attrPush token #js ["class" "external-link"]))
                (default-link-open tokens idx options env slf)))))
    (.render mdit content)))

(defn scroll-to-element-by-id
  "Scroll to element by its ID"
  [elem-id]
  (when-let [elem (js/document.getElementById elem-id)]
    (let [elem-rect (.getBoundingClientRect elem)
          scroll-top (+ js/document.scrollingElement.scrollTop elem-rect.top)]
      (js/scrollTo #js {:top scroll-top
                        :behavior "smooth"}))))

(defmacro js-swap! [var-symbol expr]
  `(set! ,var-symbol (,expr ,var-symbol)))
