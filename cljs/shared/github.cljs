(ns shared.github
  "Shared GitHub utilities for both browser and worker"
  (:require [clojure.string :as s]))

(def ^:private base-url
  "https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/")

(defn to-raw-url
  "Convert GitHub blob URL to raw content URL"
  [url]
  (-> url
      (s/replace "github.com" "raw.githubusercontent.com")
      (s/replace "/blob/" "/")))

(defn get-data-base-url
  "Get data base URL for a language"
  [lang]
  (str base-url (if (= lang :en) "data.en/" "data/")))

(defn find-post-by-md-url
  "Find a post by its md-url (the url field from index.json).
   posts should be a seq of posts (not a map).
   md-url can be:
   - Full GitHub URL: https://github.com/bolasblack/BlogPosts/blob/master/xxx.md
   - Just the filename: xxx.md
   Returns the matching post or nil"
  [posts md-url]
  (let [normalize-url (fn [url]
                        (when url
                          (-> url
                              (s/replace #"^.*/([^/]+)$" "$1")
                              (s/replace #"\.md$" ""))))]
    (some (fn [post]
            (let [post-url (:url post)]
              (when (or (= post-url md-url)
                        (= (normalize-url post-url) (normalize-url md-url)))
                post)))
          posts)))
