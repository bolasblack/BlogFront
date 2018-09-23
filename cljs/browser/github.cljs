(ns browser.github
  (:require ["octokat" :as Github]
            ["js-yaml" :as js-yaml]
            [utils.async :as ua :include-macros true]
            [clojure.string :as s]))

(defn- parse-post-filename [name]
  (->> (re-matches #"^(\d{4}-\d{2}-\d{2})-(.+)\.(?:md)$" name)
       next
       (zipmap [:date :title])))

(defprotocol IPost
  (id [post])
  (date [post])
  (title [post]))

(defrecord Post [;; 获取 contents 时就能获取的信息
                 type encoding size name path sha url git_url html_url download_url
                 ;; 获取文件时才能获取的信息
                 title content]
  IPost
  (id [post]
    (:path post))
  (date [post]
    (-> post
        :name
        parse-post-filename
        :date))
  (title [post]
    (or (:title post)
        (-> post
            :name
            parse-post-filename
            :title
            (s/replace "_" " ")))))

(def ^:private repo (.repos (Github.) "bolasblack" "BlogPosts"))

(defn get-posts []
  (ua/go-try-let [directory (ua/<p? (-> (.contents repo "")
                                      (.fetch)))
                  contents (js->clj (.-items directory) :keywordize-keys true)
                  posts (->> contents
                             (filter (comp #(not (or (s/starts-with? % "_")
                                                     (s/starts-with? % ".")))
                                           :name))
                             (map map->Post)
                             reverse)]
    posts))

(defn parse-post-content [raw-post content]
  (js* "debugger")
  (let [post (-> raw-post
                 (js->clj :keywordize-keys true)
                 map->Post)
        post (if (nil? content)
               post
               (let [[meta-str content] (->> (s/split content #"(?m)^-+$" 3)
                                             next
                                             (map s/trim))
                     meta (js->clj (js-yaml/safeLoad meta-str) :keywordize-keys true)]
                 (-> post
                     (assoc :title (:title meta))
                     (assoc :content content))))]
    post))

(defn get-post [post]
  (ua/go-try-let [raw-post (ua/<p? (-> (.contents repo (:path post))
                                       (.fetch)))]
    (parse-post-content
     raw-post
     (when (= (.-encoding raw-post) "base64")
       (ua/<p? (.read raw-post))))))
