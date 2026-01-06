(ns shared.markdown
  "Shared markdown rendering utilities for both browser and worker"
  (:require
   [clojure.string :as s]
   [goog.object :as gobj]
   ["highlight.js" :as hl]
   ["markdown-it" :as mdit-module]
   ["markdown-it-anchor" :as mdit-anchor-module]
   ["markdown-it-footnote" :as mdit-footnote-module]
   [shared.router :as router]))

(defn- md-link->blog-url
  "Convert markdown relative link to blog URL.
   ./2024-01-01-my-post.md -> /2024-01-01-my-post (or /en/2024-01-01-my-post)
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
                       (s/replace #"\.md$" ""))]
      (str (router/lang-prefix lang) (js/encodeURIComponent filename)))))

(defn render-md
  "Render markdown content with full configuration.
   Options:
   - :heading-id-renderer - function to generate heading IDs (default: encodeURIComponent)
   - :lang - language for internal links (:zh or :en)"
  [content & {:keys [heading-id-renderer lang]
              :or {heading-id-renderer #(js/encodeURIComponent %)}}]
  (let [mdit-generator (or (.-default mdit-module) mdit-module)
        mdit-anchor (or (.-default mdit-anchor-module) mdit-anchor-module)
        mdit-footnote (or (.-default mdit-footnote-module) mdit-footnote-module)
        mdit (-> (mdit-generator #js {:html true
                                      :highlight (fn [code lang-hint]
                                                   (try
                                                     (.-value (hl/highlight (s/trim code) #js {:language lang-hint}))
                                                     (catch js/Error _
                                                       (s/trim code))))})
                 (.use mdit-anchor
                       #js {:permalink (.. mdit-anchor -permalink (linkInsideHeader
                                        #js {:class "header-anchor"
                                             :symbol ""
                                             :ariaHidden true
                                             :placement "before"
                                             :renderHref #(str "#" (heading-id-renderer (js/decodeURIComponent %)))}))
                            :callback (fn [^js token ^js info]
                                        (->> (.-slug info)
                                             (js/decodeURIComponent)
                                             (heading-id-renderer)
                                             (.attrSet token "id")))})
                 (.use mdit-footnote))
        rules (.. mdit -renderer -rules)
        get-refid (fn [tokens idx options env ^js slf]
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
          (fn [tokens idx options env ^js slf]
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
                                (fn [tokens idx options env ^js slf]
                                  (.renderToken slf tokens idx options)))]
      (set! (.-link_open rules)
            (fn [tokens idx options env slf]
              (let [^js token (aget tokens idx)
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

(defn render-md-simple
  "Simple markdown rendering for SSR (no custom heading IDs)"
  [content lang]
  (render-md content :lang lang))

(defn parse-frontmatter
  "Parse markdown content, extracting YAML frontmatter.
   Returns the content without frontmatter."
  [content]
  (let [parts (s/split content #"(?m)^-+$" 3)
        [_ post-content] (when (>= (count parts) 2)
                           [(second parts) (s/trim (or (nth parts 2 nil) ""))])]
    (or post-content content)))
