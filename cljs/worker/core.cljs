(ns worker.core
  (:refer-clojure :rename {str cstr})
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [reagent.dom.server :as rdom-server]
   [shared.components :as ui]
   [shared.constants :refer [preload-state-html-id]]
   [shared.github :as gh]
   [shared.i18n :as i18n]
   [shared.markdown :as md]
   [shared.router :as router]
   [shared.state :as state]))

;; ============================================================================
;; Data Fetching
;; ============================================================================

(defn- fetch-single-page [url]
  (-> (js/fetch url)
      (.then (fn [res]
               (if (.-ok res)
                 (.json res)
                 (js/Promise.resolve nil))))
      (.catch (fn [_] nil))))

(defn- fetch-all-posts
  "Recursively fetch all pages of posts"
  [lang first-page-url all-posts]
  (-> (fetch-single-page first-page-url)
      (.then (fn [data]
               (if data
                 (let [posts (js->clj (aget data "posts") :keywordize-keys true)
                       next-file (aget data "pagination" "next-file")
                       accumulated (into all-posts posts)]
                   (if next-file
                     (fetch-all-posts lang (cstr (gh/get-data-base-url lang) next-file) accumulated)
                     (js/Promise.resolve {:posts accumulated})))
                 (js/Promise.resolve {:posts all-posts}))))))

(defn- fetch-posts-index [lang]
  (let [base-url (gh/get-data-base-url lang)
        first-page-url (cstr base-url "index.json")]
    (-> (fetch-all-posts lang first-page-url [])
        (.then (fn [{:keys [posts]}]
                 {:posts (mapv #(assoc % :lang lang) posts)})))))

(defn- fetch-post-content-by-url
  "Fetch post content from GitHub raw URL"
  [post-url]
  (when post-url
    (-> (js/fetch (gh/to-raw-url post-url))
        (.then #(if (.-ok %)
                  (.text %)
                  (js/Promise.resolve nil)))
        (.catch (fn [_] nil)))))

;; ============================================================================
;; Route Parsing (using shared module)
;; ============================================================================

(defn- parse-route [pathname]
  (let [route (router/parse-route pathname)]
    ;; Map :home to :posts for SSR
    (if (= (:type route) :home)
      (assoc route :type :posts)
      route)))

;; ============================================================================
;; SSR Components (using shared components)
;; ============================================================================

(defn- App [{:keys [posts post tag lang route-type rendered-content]}]
  [:<>
   [ui/ThemeToggle {:lang lang}]
   [:main {:role "main"}
    (case route-type
      :post [:<>
             [ui/BlogPost {:post post
                           :lang lang
                           :loading? (nil? rendered-content)
                           :content [:div.BlogPost__md {:dangerouslySetInnerHTML (r/unsafe-html rendered-content)}]}]
             [ui/FooterLinks {:lang lang :position :bottom-center}]]
      :tag [:<>
            [ui/TagPosts {:tag tag :posts posts :lang lang :loading? (empty? posts)}]
            [ui/FooterLinks {:lang lang :position :bottom-center}]]
      ;; default: posts list
      [:<>
       [ui/BlogPosts {:posts posts :lang lang :loading? (empty? posts)}]
       [ui/FooterLinks {:lang lang :position :bottom-center}]])]])

;; ============================================================================
;; HTML Rendering
;; ============================================================================

(defn- render-app-html [{:keys [posts post tag lang route-type rendered-content]}]
  (rdom-server/render-to-string
   (r/as-element [App {:posts posts
                       :post post
                       :tag tag
                       :lang lang
                       :route-type route-type
                       :rendered-content rendered-content}])))

(defn- render-meta-tags
  "Render meta tags as Hiccup and convert to HTML string"
  [{:keys [title description lang]}]
  (rdom-server/render-to-static-markup
   (r/as-element
    [:<>
     [:title title]
     [:link {:rel "alternate"
             :type "application/rss+xml"
             :href (ui/rss-url lang)}]
     [:meta {:name "description" :content description}]
     [:meta {:property "og:title" :content title}]
     [:meta {:property "og:description" :content description}]
     [:meta {:property "og:type" :content "article"}]
     [:meta {:name "twitter:card" :content "summary"}]
     [:meta {:name "twitter:title" :content title}]
     [:meta {:name "twitter:description" :content description}]])))

(defn- inject-meta-tags [html {:keys [title description lang]}]
  (let [lang-str (if (= lang :en) "en" "zh")
        meta-tags (render-meta-tags {:title title :description description :lang lang})]
    (-> html
        (str/replace #"<html>" (cstr "<html lang=\"" lang-str "\">"))
        (str/replace #"<head>" (cstr "<head>" meta-tags)))))

(defn- inject-ssr-content [html app-html]
  (str/replace html
               #"<div id=\"app\" class=\"app-container\"></div>"
               (cstr "<div id=\"app\" class=\"app-container\">" app-html "</div>")))

(defn- prepare-ssr-state
  "Prepare state data for client hydration.
   Returns a plain map (no conversion needed for JSON serialization)."
  [{:keys [route posts post raw-content]}]
  (let [lang (:lang route)]
    (case (:type route)
      :post
      {:current-lang lang
       :visiting-post (when post
                        (assoc post :content (or raw-content "")))}

      :tag
      {:current-lang lang
       :visiting-tag (:id route)
       :tag-posts posts}

      ;; default: home/posts list
      {:current-lang lang
       :posts (zipmap (map :id posts) posts)})))

(defn- inject-ssr-state
  "Inject SSR state as JSON in a script tag for client hydration"
  [html ssr-state]
  (let [state-json (js/JSON.stringify (state/state->js ssr-state))
        script-tag (cstr "<script id=\"" preload-state-html-id "\" type=\"application/json\">"
                         state-json
                         "</script>")]
    (str/replace html "</head>" (cstr script-tag "</head>"))))

;; ============================================================================
;; Request Handler
;; ============================================================================
;;
;; Note: Static assets are handled directly by Cloudflare Assets binding
;; via wrangler.toml `run_worker_first` configuration. This worker only
;; receives requests for HTML pages that need SSR.
;; ============================================================================

(defn- get-assets [env]
  (aget env "ASSETS"))

(defn- find-post-by-id [posts post-id]
  (some #(when (= (:id %) post-id) %) posts))

(defn- fetch-and-render-post-content [post lang]
  (-> (fetch-post-content-by-url (:url post))
      (.then (fn [raw-content]
               (when raw-content
                 (let [content-no-fm (md/parse-frontmatter raw-content)]
                   {:raw content-no-fm
                    :rendered (md/render-md content-no-fm
                                            :heading-id-renderer #(router/heading-url (:id post) % lang)
                                            :lang lang)}))))))

(defn- get-page-meta [route post]
  (let [lang (:lang route)
        t #(i18n/t lang %1 %2)]
    (case (:type route)
      :post {:title (t :seo/post-title (:title post))
             :description (:title post)}
      :tag {:title (t :seo/tag-title (:id route))
            :description (t :seo/tag-desc (:id route))}
      ;; default: home
      {:title (t :seo/site-title nil)
       :description (t :seo/site-desc nil)})))

(defn- build-html-response [base-html {:keys [route posts post raw-content rendered-content]}]
  (let [{:keys [title description]} (get-page-meta route post)
        app-html (render-app-html {:posts posts
                                   :post post
                                   :tag (:id route)
                                   :lang (:lang route)
                                   :route-type (:type route)
                                   :rendered-content rendered-content})
        ssr-state (prepare-ssr-state {:route route
                                      :posts posts
                                      :post post
                                      :raw-content raw-content
                                      :rendered-content rendered-content})
        final-html (-> base-html
                       (inject-meta-tags {:title title
                                          :description description
                                          :lang (:lang route)})
                       (inject-ssr-content app-html)
                       (inject-ssr-state ssr-state))]
    (js/Response. final-html
                  #js {:headers #js {"Content-Type" "text/html;charset=UTF-8"}})))

(defn- fetch-base-html [assets request-url]
  (-> (.fetch assets (js/Request. (js/URL. "/index.html" request-url)))
      (.then #(.text %))))

(defn- filter-posts-by-tag [posts tag]
  (filter #(some #{tag} (:tags %)) posts))

(defn- handle-goto-article [route posts]
  (when-let [post (gh/find-post-by-md-url posts (:md-url route))]
    (let [redirect-url (router/blog-url (:id post) (:lang route))]
      (js/Response. nil #js {:status 302
                             :headers #js {"Location" redirect-url}}))))

(defn- handle-request [request env]
  (let [assets (get-assets env)
        pathname (.-pathname (js/URL. (.-url request)))
        route (parse-route pathname)]
    (-> (js/Promise.all #js [(fetch-base-html assets (.-url request))
                             (fetch-posts-index (:lang route))])
        (.then
         (fn [[base-html {:keys [posts]}]]
           (case (:type route)
             ;; Redirect to the correct article URL
             :goto-article
             (or (handle-goto-article route posts)
                 ;; Fallback to home if post not found
                 (js/Response. nil #js {:status 302
                                        :headers #js {"Location" (router/home-url (:lang route))}}))

             :post
             (let [post (find-post-by-id posts (:id route))]
               (if (and post (:url post))
                 (-> (fetch-and-render-post-content post (:lang route))
                     (.then (fn [{:keys [raw rendered]}]
                              (build-html-response base-html
                                                   {:route route
                                                    :posts posts
                                                    :post post
                                                    :raw-content raw
                                                    :rendered-content rendered}))))
                 (build-html-response base-html
                                      {:route route
                                       :posts posts
                                       :post nil
                                       :rendered-content nil})))

             :tag
             (build-html-response base-html
                                  {:route route
                                   :posts (filter-posts-by-tag posts (:id route))
                                   :post nil
                                   :rendered-content nil})

             ;; default: posts list
             (build-html-response base-html
                                  {:route route
                                   :posts posts
                                   :post nil
                                   :rendered-content nil}))))
        (.catch
         (fn [err]
           (js/console.error "SSR Error:" err)
           (.fetch assets request))))))

;; ============================================================================
;; Worker Export
;; ============================================================================

(def ^:export default
  #js {:fetch (fn [request env _ctx]
                (handle-request request env))})
