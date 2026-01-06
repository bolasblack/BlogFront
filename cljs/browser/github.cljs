(ns browser.github
  (:require
   [rxcljs.core :as rc :include-macros true]
   [rxcljs.transformers :as rt :include-macros true]
   [clojure.string :as s]
   [shared.github :as gh]
   [shared.markdown :as md]
   [shared.router :as router]))

(defn- get-index-url
  "Get index URL for a language"
  [lang]
  (str (gh/get-data-base-url lang) "index.json"))

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
;; Additional field: content (loaded when viewing post), post-lang (language)
(defrecord Post [path post-date post-title post-id url tags
                 post-created-at post-updated-at content post-lang]
  IPost
  (id [_] post-id)
  (date [_] post-date)
  (title [_] post-title)
  (created-at [_] post-created-at)
  (updated-at [_] post-updated-at)
  (post-tags [_] tags)
  (blog-url [_] (router/blog-url post-id post-lang))
  (heading-id [_ heading-text] (router/heading-url post-id heading-text post-lang)))

(defn- index-post->Post
  "Convert a post entry from index.json to Post record"
  ([entry] (index-post->Post entry nil))
  ([entry lang]
   (map->Post {:path (:path entry)
               :post-date (:date entry)
               :post-title (:title entry)
               :post-id (:id entry)
               :url (:url entry)
               :tags (:tags entry)
               :post-created-at (:created-at entry)
               :post-updated-at (:updated-at entry)
               :post-lang lang})))

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
  ([] (get-posts nil))
  ([lang]
   (rc/go-loop [url (get-index-url lang)
                all-posts []]
     (let [{:keys [posts next-file]} (rc/<! (fetch-index-page url))
           accumulated (into all-posts posts)]
       (if next-file
         (recur (str (gh/get-data-base-url lang) next-file) accumulated)
         (map #(index-post->Post % lang) accumulated))))))

(defn get-post
  "Fetch post content using the URL from index.json"
  [post]
  (rc/go-let [raw-url (gh/to-raw-url (:url post))
              response (rt/<p! (js/fetch raw-url))
              content (rt/<p! (.text response))]
    (assoc post :content (md/parse-frontmatter content))))

(defn get-tag-posts
  "Fetch posts for a specific tag"
  ([tag] (get-tag-posts tag nil))
  ([tag lang]
   (rc/go-let [tag-file (str "tags/" (s/lower-case tag) ".json")
               url (str (gh/get-data-base-url lang) tag-file)
               response (rt/<p! (js/fetch url))
               data (rt/<p! (.json response))
               js-data (js->clj data :keywordize-keys true)]
     {:tag (:tag js-data)
      :count (:count js-data)
      :posts (map #(index-post->Post % lang) (:posts js-data))})))

