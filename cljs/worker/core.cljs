(ns worker.core
  (:require
   [clojure.string :as s]
   [reagent.core :as r]
   [reagent.dom.server :as rdom-server]
   [shared.components :as ui]
   [shared.github :as gh]
   [shared.i18n :as i18n]
   [shared.markdown :as md]
   [shared.router :as router]))

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
  [base-url url all-posts]
  (-> (fetch-single-page url)
      (.then (fn [data]
               (if data
                 (let [posts (js->clj (aget data "posts") :keywordize-keys true)
                       next-file (aget data "pagination" "next-file")
                       accumulated (into all-posts posts)]
                   (if next-file
                     (fetch-all-posts base-url (str base-url next-file) accumulated)
                     (js/Promise.resolve {:posts accumulated})))
                 (js/Promise.resolve {:posts all-posts}))))))

(defn- fetch-posts-index [lang]
  (let [base-url (gh/get-data-base-url lang)
        url (str base-url "index.json")]
    (fetch-all-posts base-url url [])))

(defn- fetch-post-content-by-url
  "Fetch post content from GitHub raw URL"
  [post-url]
  (when post-url
    (-> (js/fetch (gh/to-raw-url post-url))
        (.then (fn [res]
                 (if (.-ok res)
                   (.text res)
                   (js/Promise.resolve nil))))
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
             [ui/FooterLinks {:lang lang :position :bottom-right}]]
      :tag [:<>
            [ui/TagPosts {:tag tag :posts posts :lang lang :loading? (empty? posts)}]
            [ui/FooterLinks {:lang lang :position :bottom-right}]]
      ;; default: posts list
      [:<>
       [ui/FooterLinks {:lang lang :position :top-right}]
       [ui/BlogPosts {:posts posts :lang lang :loading? (empty? posts)}]])]])

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
  [{:keys [title description]}]
  (rdom-server/render-to-static-markup
   (r/as-element
    [:<>
     [:title title]
     [:meta {:name "description" :content description}]
     [:meta {:property "og:title" :content title}]
     [:meta {:property "og:description" :content description}]
     [:meta {:property "og:type" :content "article"}]
     [:meta {:name "twitter:card" :content "summary"}]
     [:meta {:name "twitter:title" :content title}]
     [:meta {:name "twitter:description" :content description}]])))

(defn- inject-meta-tags [html {:keys [title description lang]}]
  (let [lang-str (if (= lang :en) "en" "zh")
        meta-tags (render-meta-tags {:title title :description description})]
    (-> html
        (s/replace #"<html>" (str "<html lang=\"" lang-str "\">"))
        (s/replace #"<head>" (str "<head>" meta-tags)))))

(defn- inject-ssr-content [html app-html]
  (s/replace html
             #"<div id=\"app\" class=\"app-container\"></div>"
             (str "<div id=\"app\" class=\"app-container\">" app-html "</div>")))

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
                 (md/render-md-simple (md/parse-frontmatter raw-content) lang))))))

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

(defn- build-html-response [base-html {:keys [route posts post rendered-content]}]
  (let [{:keys [title description]} (get-page-meta route post)
        app-html (render-app-html {:posts posts
                                   :post post
                                   :tag (:id route)
                                   :lang (:lang route)
                                   :route-type (:type route)
                                   :rendered-content rendered-content})
        final-html (-> base-html
                       (inject-meta-tags {:title title
                                          :description description
                                          :lang (:lang route)})
                       (inject-ssr-content app-html))]
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
                     (.then #(build-html-response base-html
                                                  {:route route
                                                   :posts posts
                                                   :post post
                                                   :rendered-content %})))
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
