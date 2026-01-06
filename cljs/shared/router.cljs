(ns shared.router
  "Shared routing utilities for URL generation and parsing"
  (:require [clojure.string :as s]))

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
  ([tag] (tag-url tag :zh))
  ([tag lang]
   (str (lang-prefix lang) "tag/" (js/encodeURIComponent tag))))

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
        path (s/replace pathname #"^/" "")
        en-prefix? (s/starts-with? path "en/")
        lang (if en-prefix? :en :zh)
        ;; Strip language prefix for further parsing
        clean-path (if en-prefix? (subs path 3) path)]
    (cond
      ;; Goto article route: /goto/articles/{mdUrl}
      (s/starts-with? clean-path "goto/articles/")
      {:type :goto-article
       :md-url (js/decodeURIComponent (subs clean-path 15))
       :lang lang}

      ;; Tag route: /tag/tag-name
      (s/starts-with? clean-path "tag/")
      {:type :tag
       :id (js/decodeURIComponent (subs clean-path 4))
       :lang lang}

      ;; Home route: empty path
      (empty? clean-path)
      {:type :home
       :lang lang}

      ;; Post route: /post-id with optional #heading
      :else
      (let [heading (when (and hash (not (s/blank? hash)))
                      (js/decodeURIComponent (s/replace hash #"^#" "")))]
        {:type :post
         :id (js/decodeURIComponent clean-path)
         :heading heading
         :lang lang}))))

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
  [hash lang]
  (when (and hash (s/starts-with? hash "#/"))
    (let [hash-path (subs hash 2) ;; Remove "#/"
          ;; Check for language prefix in hash
          en-prefix? (s/starts-with? hash-path "en/")
          hash-lang (if en-prefix? :en lang)
          clean-path (if en-prefix? (subs hash-path 3) hash-path)]
      (cond
        ;; Tag route in hash: #/tag/tag-name or #/en/tag/tag-name
        (s/starts-with? clean-path "tag/")
        (tag-url (js/decodeURIComponent (subs clean-path 4)) hash-lang)

        ;; Empty path means home
        (empty? clean-path)
        (home-url hash-lang)

        ;; Post route: #/post-id or #/en/post-id
        :else
        (blog-url (js/decodeURIComponent clean-path) hash-lang)))))
