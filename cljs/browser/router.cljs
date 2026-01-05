(ns browser.router
  (:require [clojure.string :as s]))

;; =============================================================================
;; URL Generation
;; =============================================================================

(defn blog-url
  "Generate blog URL for a post"
  [post-id lang]
  (let [prefix (if (= lang :en) "#/en/" "#/")]
    (str prefix (js/encodeURIComponent post-id))))

(defn heading-url
  "Generate URL with heading anchor"
  [post-id heading-text lang]
  (let [prefix (if (= lang :en) "/en/" "/")]
    (str prefix (js/encodeURIComponent post-id) "/" (js/encodeURIComponent heading-text))))

(defn tag-url
  "Generate URL for a tag page"
  ([tag] (tag-url tag :zh))
  ([tag lang]
   (let [prefix (if (= lang :en) "#/en/tag/" "#/tag/")]
     (str prefix (js/encodeURIComponent tag)))))

(defn home-url
  "Generate home URL for a language"
  [lang]
  (if (= lang :en) "#/en/" "#/"))

;; =============================================================================
;; URL Parsing
;; =============================================================================

(defn parse-url-hash
  "Parse URL hash to determine route type
   Returns: {:type :home :lang :zh|:en} | {:type :post :post-id str :heading str :heading-id str :lang :zh|:en} | {:type :tag :tag str :lang :zh|:en} | {:type :goto-article :md-url str :lang :zh|:en}"
  [url-hash]
  (let [;; Check for language prefix
        en-prefix? (s/starts-with? url-hash "#/en/")
        lang (if en-prefix? :en :zh)
        ;; Strip language prefix for further parsing
        path (if en-prefix?
               (str "#/" (subs url-hash 5))  ;; "#/en/" is 5 chars
               url-hash)]
    (cond
      ;; Goto article route: #/goto/articles/{mdUrl} or #/en/goto/articles/{mdUrl}
      (s/starts-with? path "#/goto/articles/")
      {:type :goto-article
       :md-url (js/decodeURIComponent (subs path 16))  ;; "#/goto/articles/" is 16 chars
       :lang lang}

      ;; Tag route: #/tag/tag-name or #/en/tag/tag-name
      (s/starts-with? path "#/tag/")
      {:type :tag
       :tag (js/decodeURIComponent (subs path 6))
       :lang lang}

      ;; English home: #/en/ (after stripping becomes #/)
      (and en-prefix? (= path "#/"))
      {:type :home
       :lang lang}

      ;; Post route: #/post-id or #/post-id/heading
      (re-matches #"^#?/([^/]+)(?:/(.*)$)?" path)
      (let [matches (re-matches #"^#?/([^/]+)(?:/(.*)$)?" path)]
        {:type :post
         :post-id (js/decodeURIComponent (nth matches 1))
         :heading (when (nth matches 2 nil)
                    (js/decodeURIComponent (nth matches 2)))
         :heading-id (s/replace url-hash #"^#?" "")
         :lang lang})

      ;; Home route
      :else
      {:type :home
       :lang lang})))

(defn parse-post-heading-id
  "str -> nil | {:post-id str :heading str :heading-id str}"
  [url-hash]
  (let [parsed (parse-url-hash url-hash)]
    (when (= (:type parsed) :post)
      (select-keys parsed [:post-id :heading :heading-id]))))

;; =============================================================================
;; Navigation History
;; =============================================================================

(defn get-nav-depth
  "Get navigation depth from history state"
  []
  (or (when js/history.state (aget js/history.state "blog-nav-depth")) 0))

(defn update-nav-depth!
  "Update navigation depth in session storage and history state"
  []
  (if (and js/history.state (aget js/history.state "blog-nav-depth"))
    ;; Back navigation or refresh - restore from history state
    (js/sessionStorage.setItem "blog-current-depth"
                               (aget js/history.state "blog-nav-depth"))
    ;; Forward navigation - increment and save
    (let [current-depth (js/parseInt
                         (or (js/sessionStorage.getItem "blog-current-depth") "0")
                         10)
          new-depth (inc current-depth)]
      (js/sessionStorage.setItem "blog-current-depth" new-depth)
      (js/history.replaceState #js {:blog-nav-depth new-depth} ""))))

(defn go-back!
  "Navigate back or to home if at the beginning of history"
  [e lang]
  (.preventDefault e)
  (if (> (get-nav-depth) 1)
    (js/history.back)
    (set! js/location.hash (home-url lang))))
