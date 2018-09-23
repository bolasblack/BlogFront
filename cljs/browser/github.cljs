(ns browser.github
  (:require ["octokat" :as Github]
            [utils.async :as a :include-macros true]
            [clojure.string :as s]))

(defn- parse-post-filename [name]
  (->> (re-matches #"^(\d{4}-\d{2}-\d{2})-(.+)\.(?:md)$" name)
       next
       (zipmap [:date :title])))

(defprotocol IPost
  (date [post])
  (title [post]))

(defrecord Post [type encoding size name path content sha url git_url html_url download_url]
  IPost
  (date [post]
    (-> post
        :name
        parse-post-filename
        :date))
  (title [post]
    (-> post
        :name
        parse-post-filename
        :title
        (s/replace "_" " "))))

(def ^:private repo (.repos (Github.) "bolasblack" "BlogPosts"))

(defn get-posts []
  (a/go-let [resp (a/<p? (-> (.contents repo "")
                             (.fetch)))
             contents (js->clj (.-items resp) :keywordize-keys true)
             posts (->> contents
                        (filter (comp #(not (or (s/starts-with? % "_")
                                                (s/starts-with? % ".")))
                                      :name))
                        (map map->Post)
                        reverse)]
    posts))
