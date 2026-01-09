(ns browser.github
  (:require
   [rxcljs.core :as rc :include-macros true]
   [rxcljs.transformers :as rt :include-macros true]
   [clojure.string :as s]
   [shared.github :as gh]
   [shared.markdown :as md]))

(defn- get-index-url
  "Get index URL for a language"
  [lang]
  (str (gh/get-data-base-url lang) "index.json"))

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
         (recur (str (gh/get-data-base-url lang) next-file)
                accumulated)
         (map #(assoc % :lang lang) accumulated))))))

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
      :posts (map #(assoc % :lang lang) (:posts js-data))})))
