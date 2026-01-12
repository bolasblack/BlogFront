(ns shared.markdown
  "Shared markdown rendering utilities for both browser and worker"
  (:refer-clojure :rename {str cstr})
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   ["highlight.js" :default hljs]
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
             (str/ends-with? href ".md")
             ;; Must NOT be an absolute URL
             (not (str/starts-with? href "http://"))
             (not (str/starts-with? href "https://"))
             ;; Must be a relative path (starts with ./, ../, or just filename)
             (re-matches #"^\.{0,2}/?.+\.md$" href))
    (let [;; Extract filename without path and extension
          filename (-> href
                       (str/replace #"^\.{0,2}/" "")
                       (str/replace #"\.md$" ""))]
      (cstr (router/lang-prefix lang) (js/encodeURIComponent filename)))))

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
        ^js mdit (-> (mdit-generator #js {:html true
                                          :langPrefix "hljs language-"
                                          :highlight (fn [code lang-hint]
                                                       (try
                                                         (let [trimmed (str/trim code)
                                                               lang (when (and lang-hint
                                                                               (not (str/blank? lang-hint))
                                                                               (.getLanguage hljs lang-hint))
                                                                      lang-hint)
                                                               result (if lang
                                                                        (.highlight hljs trimmed #js {:language lang})
                                                                        (.highlightAuto hljs trimmed))]
                                                           (.-value result))
                                                         (catch js/Error _
                                                           "")))})
                     (.use mdit-anchor
                           #js {:permalink (.. mdit-anchor -permalink (linkInsideHeader
                                                                       #js {:class "header-anchor"
                                                                            :symbol ""
                                                                            :ariaHidden true
                                                                            :placement "before"
                                                                            :renderHref #(cstr "#" (heading-id-renderer (js/decodeURIComponent %)))}))
                                :callback (fn [^js token ^js info]
                                            (->> (.-slug info)
                                                 (js/decodeURIComponent)
                                                 (heading-id-renderer)
                                                 (.attrSet token "id")))})
                     (.use mdit-footnote))
        ^js rules (.. mdit -renderer -rules)
        default-fence (.-fence rules)
        get-refid (fn [tokens idx options env ^js slf]
                    (let [id (.. slf -rules (footnote_anchor_name tokens idx options env slf))
                          subid (gobj/getValueByKeys tokens idx "meta" "subId")
                          refid (if (> subid 0) (cstr id ":" subid) id)]
                      refid))]
    (set! (.-fence rules)
          (fn [tokens idx options env slf]
            (let [html (default-fence tokens idx options env slf)]
              (cond
                (str/includes? html "<code class=\"")
                (str/replace html "<code class=\"" "<code class=\"hljs ")
                (str/includes? html "<code>")
                (str/replace html "<code>" "<code class=\"hljs\">")
                :else html))))
    (set! (.-footnote_anchor rules)
          (fn [tokens idx options env slf]
            (let [refid (get-refid tokens idx options env slf)]
              (cstr "<a "
                    "href='#" (heading-id-renderer (cstr "fnref" refid)) "' "
                    "class='footnote-backref'>\u21a9\uFE0E</a>"))))
    (set! (.-footnote_open rules)
          (fn [tokens idx options env slf]
            (let [refid (get-refid tokens idx options env slf)]
              (cstr "<li id='" (heading-id-renderer (cstr "fn" refid)) "' "
                    "class='footnote-item'>"))))
    (set! (.-footnote_ref rules)
          (fn [tokens idx options env ^js slf]
            (let [refid (get-refid tokens idx options env slf)
                  caption (.. slf -rules (footnote_caption tokens idx options env slf))]
              (cstr "<sup class='footnote-ref'>"
                    "<a href='#" (heading-id-renderer (cstr "fn" refid)) "' "
                    "id=" (heading-id-renderer (cstr "fnref" refid)) " "
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
                                      (or (str/starts-with? href "http://")
                                          (str/starts-with? href "https://")))
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
    (.render ^js mdit content)))

(defn parse-frontmatter
  "Parse markdown content, extracting YAML frontmatter.
   Returns the content without frontmatter."
  [content]
  (let [parts (str/split content #"(?m)^-+$" 3)
        [_ post-content] (when (>= (count parts) 2)
                           [(second parts) (str/trim (or (nth parts 2 nil) ""))])]
    (or post-content content)))
