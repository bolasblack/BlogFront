(ns browser.github
  (:require
   ["js-yaml" :as js-yaml]
   [rxcljs.core :as rc :include-macros true]
   [rxcljs.transformers :as rt :include-macros true]
   [clojure.string :as s]))

(def ^:private INDEX_URL
  "https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/data/index.json")

(defprotocol IPost
  (id [post])
  (date [post])
  (title [post])
  (created-at [post])
  (updated-at [post])
  (post-tags [post])
  (blog-url [post])
  (heading-id [post heading-text]))

;; Post record for data from index.json
;; Additional field: content (loaded when viewing post)
(defrecord Post [path post-date post-title post-id url tags
                 post-created-at post-updated-at content]
  IPost
  (id [_] post-id)
  (date [_] post-date)
  (title [_] post-title)
  (created-at [_] post-created-at)
  (updated-at [_] post-updated-at)
  (post-tags [_] tags)
  (blog-url [_]
    (str "#/" (js/encodeURIComponent post-id)))
  (heading-id [_ heading-text]
    (str "/" (js/encodeURIComponent post-id) "/" (js/encodeURIComponent heading-text))))

(defn tag-url
  "Generate URL for a tag page"
  [tag]
  (str "#/tag/" (js/encodeURIComponent tag)))

(defn- index-post->Post
  "Convert a post entry from index.json to Post record"
  [entry]
  (map->Post {:path (:path entry)
              :post-date (:date entry)
              :post-title (:title entry)
              :post-id (:id entry)
              :url (:url entry)
              :tags (:tags entry)
              :post-created-at (:created-at entry)
              :post-updated-at (:updated-at entry)}))

(def ^:private INDEX_BASE_URL
  "https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/data/")

(defn- fetch-index-page
  "Fetch a single index page and return {:posts [...] :next-file ...}"
  [url]
  (rc/go-let [response (rt/<p! (js/fetch url))
              data (rt/<p! (.json response))
              js-data (js->clj data :keywordize-keys true)]
    {:posts (:posts js-data)
     :next-file (get-in js-data [:pagination :next-file])}))

(defn get-posts
  "Fetch all posts from static index.json, handling pagination"
  []
  (rc/go-loop [url INDEX_URL
               all-posts []]
    (let [{:keys [posts next-file]} (rc/<! (fetch-index-page url))
          accumulated (into all-posts posts)]
      (if next-file
        (recur (str INDEX_BASE_URL next-file) accumulated)
        (map index-post->Post accumulated)))))

(defn- parse-markdown-content
  "Parse markdown content, extracting YAML frontmatter"
  [content]
  (let [[meta-str post-content] (->> (s/split content #"(?m)^-+$" 3)
                                     next
                                     (map s/trim))]
    {:meta (when meta-str
             (js->clj (js-yaml/load meta-str) :keywordize-keys true))
     :content post-content}))

(defn- to-raw-url
  "Convert GitHub blob URL to raw content URL"
  [url]
  (-> url
      (s/replace "github.com" "raw.githubusercontent.com")
      (s/replace "/blob/" "/")))

(defn get-post
  "Fetch post content using the URL from index.json"
  [post]
  (rc/go-let [raw-url (to-raw-url (:url post))
              response (rt/<p! (js/fetch raw-url))
              content (rt/<p! (.text response))
              parsed (parse-markdown-content content)]
    (assoc post :content (:content parsed))))

(defn get-tag-posts
  "Fetch posts for a specific tag"
  [tag]
  (rc/go-let [tag-file (str "tags/" (s/lower-case tag) ".json")
              url (str INDEX_BASE_URL tag-file)
              response (rt/<p! (js/fetch url))
              data (rt/<p! (.json response))
              js-data (js->clj data :keywordize-keys true)]
    {:tag (:tag js-data)
     :count (:count js-data)
     :posts (map index-post->Post (:posts js-data))}))

(defn parse-url-hash
  "Parse URL hash to determine route type
   Returns: {:type :home} | {:type :post :post-id str :heading str :heading-id str} | {:type :tag :tag str}"
  [url-hash]
  (cond
    ;; Tag route: #/tag/tag-name
    (s/starts-with? url-hash "#/tag/")
    {:type :tag
     :tag (js/decodeURIComponent (subs url-hash 6))}

    ;; Post route: #/post-id or #/post-id/heading
    (re-matches #"^#?/([^/]+)(?:/(.*)$)?" url-hash)
    (let [matches (re-matches #"^#?/([^/]+)(?:/(.*)$)?" url-hash)]
      {:type :post
       :post-id (js/decodeURIComponent (nth matches 1))
       :heading (when (nth matches 2 nil)
                  (js/decodeURIComponent (nth matches 2)))
       :heading-id (s/replace url-hash #"^#?" "")})

    ;; Home route
    :else
    {:type :home}))

;; Legacy function for backward compatibility
(defn parse-post-heading-id
  "str -> nil | {:post-id str :heading str :heading-id str}"
  [url-hash]
  (let [parsed (parse-url-hash url-hash)]
    (when (= (:type parsed) :post)
      (select-keys parsed [:post-id :heading :heading-id]))))
