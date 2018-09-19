(ns browser.github
  (:require ["octokat" :as Github]
            [utils.async :as a :include-macros true]))

(def ^:private repo (.repos (Github.) "bolasblack" "BlogPosts"))

(defrecord Content
    [type
     encoding
     size
     name
     path
     content
     sha
     url
     git_url
     html_url
     download_url])

(defn get-posts []
  (a/go-let [resp (a/<p? (-> (.contents repo "")
                             (.fetch)))
             contents (js->clj (.-items resp) :keywordize-keys true)
             posts (map map->Content contents)]
    posts))
