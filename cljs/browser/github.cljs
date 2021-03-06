(ns browser.github
  (:require
   ["octokat" :as Github]
   ["js-yaml" :as js-yaml]
   [rxcljs.core :as rc :include-macros true]
   [rxcljs.transformers :as rt :include-macros true]
   [clojure.string :as s]))

(defn- parse-post-filename [name]
  (->> (re-matches #"^(\d{4}-\d{2}-\d{2})-(.+)\.(?:md)$" name)
       next
       (zipmap [:date :title])))

(defprotocol IPost
  (id [post])
  (date [post])
  (title [post])
  (blog-url [post])
  (heading-id [post heading-text]))

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
            (s/replace "_" " "))))
  (blog-url [post]
    (str "#/" (js/encodeURIComponent (id post))))
  (heading-id [post heading-text]
    (str "/" (js/encodeURIComponent (id post)) "/" (js/encodeURIComponent heading-text))))

(def ^:private repo (.repos (Github.) "bolasblack" "BlogPosts"))

(defn get-posts []
  (rc/go-let [directory (rt/<p! (-> (.contents repo "")
                                    (.fetch)))
              contents (js->clj (.-items directory) :keywordize-keys true)
              posts (->> contents
                         (filter (comp #(not (or (s/starts-with? % "_")
                                                 (s/starts-with? % ".")))
                                       :name))
                         (map map->Post))]
    posts))

(defn parse-post-content [raw-post content]
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
  (rc/go-let [raw-post (rt/<p! (-> (.contents repo (:path post))
                                   (.fetch)))]
    (parse-post-content
     raw-post
     (when (= (.-encoding raw-post) "base64")
       (rt/<p! (.read raw-post))))))

(defn parse-post-heading-id
  "str -> nil | {:post-id str :heading str :heading-id str}"
  [url-hash]
  (when-let [matches (re-matches #"^#?/([^/]+)(?:/(.*)$)?" url-hash)]
    (->> matches
         (drop 1)
         (map js/decodeURIComponent)
         (zipmap [:post-id :heading])
         (merge {:heading-id (s/replace url-hash #"^#?" "")}))))
