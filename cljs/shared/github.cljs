(ns shared.github
  "Shared GitHub utilities for both browser and worker"
  (:require [clojure.string :as str]
            [shared.constants :as constants]))

(defn to-raw-url
  "Convert GitHub blob URL to raw content URL"
  [url]
  (-> url
      (str/replace "github.com" "raw.githubusercontent.com")
      (str/replace "/blob/" "/")))

(defn get-data-base-url
  "Get data base URL for a language"
  [lang]
  (str constants/blog-system-meta-url (if (= lang :en) "data.en/" "data/")))

(defn find-post-by-md-url
  "Find a post by its md-url (the url field from index.json).
   posts should be a seq of posts (not a map).
   md-url can be:
   - Full GitHub URL: https://github.com/bolasblack/BlogPosts/blob/master/xxx.md
   - Just the filename: xxx.md
   Returns the matching post or nil"
  [posts md-url]
  (let [normalize-url #(-> %
                           (str/replace #"^.*/([^/]+)$" "$1")
                           (str/replace #"\.md$" ""))]
    (some #(let [post-url (:url %)]
             (when (or (= post-url md-url)
                       (= (normalize-url post-url)
                          (normalize-url md-url)))
               %))
          posts)))

;; =============================================================================
;; Post Schema (malli)
;; =============================================================================

(def PostId
  "Schema for a post ID"
  ;; (e.g. "2024-01-01-my-post")
  [:re #"^[0-9]{4}-[0-9]{2}-[0-9]{2}-[^\s]+$"])

(def ^:private PostFieldsBasic
  "Schema for a blog post (basic fields)"
  [;; Post ID
   [:id PostId]
   ;; Language
   [:lang [:enum :zh :en]]
   ;; Post date (e.g. "2024-01-01")
   [:date :string]
   ;; Post title
   [:title :string]
   ;; GitHub URL to markdown file
   [:url :string]
   ;; List of tags
   [:tags [:sequential :string]]
   ;; ISO timestamp
   [:created-at :string]
   ;; ISO timestamp
   [:updated-at :string]])

(def Post
  "Schema for a blog post"
  (into [:map]
        PostFieldsBasic))

(def PostDetailed
  "Schema for a detailed blog post"
  (-> [:map]
      (into PostFieldsBasic)
      (into [;; Markdown content
             [:content :string]])))