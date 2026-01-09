(ns shared.router
  "Shared routing utilities for URL generation and parsing"
  (:require [clojure.string :as str]
            [malli.core :as m]
            [shared.github :as gh]))

(def RouteType
  "Route type for URL generation and parsing"
  [:enum
   :goto-article
   :home
   :post
   :tag])

;; =============================================================================
;; URL Generation
;; =============================================================================

(defn lang-prefix
  "Get URL prefix for a language"
  [lang]
  (if (= lang :en) "/en/" "/"))

(defn blog-url
  "Generate blog URL for a post"
  [post-id lang]
  (str (lang-prefix lang) (js/encodeURIComponent post-id)))

(defn heading-url
  "Generate URL with heading anchor"
  [post-id heading-text lang]
  (str (lang-prefix lang)
       (js/encodeURIComponent post-id)
       "#"
       (js/encodeURIComponent heading-text)))

(defn tag-url
  "Generate URL for a tag page"
  [tag lang]
  (str (lang-prefix lang) "tag/" (js/encodeURIComponent tag)))

(defn home-url
  "Generate home URL for a language"
  [lang]
  (lang-prefix lang))

;; =============================================================================
;; URL Parsing
;; =============================================================================

(defn parse-pathname
  "Parse URL pathname to determine route type
   Returns: {:type :home|:post|:tag|:goto-article :lang :zh|:en ...}"
  [pathname hash]
  (let [;; Remove leading slash and check for language prefix
        path (str/replace pathname #"^/" "")
        en-prefix? (str/starts-with? path "en/")
        lang (if en-prefix? :en :zh)
        ;; Strip language prefix for further parsing
        clean-path (if en-prefix? (subs path 3) path)]
    (cond
      ;; Goto article route: /goto/articles/{mdUrl}
      (str/starts-with? clean-path "goto/articles/")
      {:type :goto-article
       :lang lang
       :md-url (js/decodeURIComponent (subs clean-path 15))}

      ;; Tag route: /tag/tag-name
      (str/starts-with? clean-path "tag/")
      {:type :tag
       :lang lang
       :id (js/decodeURIComponent (subs clean-path 4))}

      ;; Post route: /post-id with optional #heading
      (nil? (m/explain gh/PostId (js/decodeURIComponent clean-path)))
      (let [post-id (js/decodeURIComponent clean-path)
            heading (when (and hash (not (str/blank? hash)))
                      (js/decodeURIComponent (str/replace hash #"^#" "")))]
        {:type :post
         :lang lang
         :id post-id
         :heading heading})

      ;; Home route: empty path
      (empty? clean-path)
      {:type :home
       :lang lang}

      :else
      {:type :home
       :lang lang})))

(defn parse-route
  "Parse pathname for SSR (no hash support)"
  [pathname]
  (parse-pathname pathname nil))

;; =============================================================================
;; Legacy Hash Route Detection
;; =============================================================================

(defn parse-legacy-hash
  "Parse legacy hash-based route and return redirect URL if applicable.
   Old format: /#/post-id or /#/tag/tag-name or /#/en/post-id
   Returns nil if hash is not a legacy route, otherwise returns the new URL."
  [hash]
  (when (and hash (str/starts-with? hash "#/"))
    (let [substr (subs hash 2) ;; Remove "#/"
          [pathname hash] (str/split substr "#")
          parsed-type (:type (parse-pathname pathname hash))]
      (if (= parsed-type :other) nil substr))))
