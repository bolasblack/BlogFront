(ns browser.core
  (:require
   [reagent.core :as r]
   [reagent.dom.client :as rdomc]
   [browser.utils :refer [dom-ready classnames render-md]]
   [browser.github :as g]
   [browser.i18n :as i18n]
   [browser.router :as router]
   [browser.state :as st]
   [browser.state-effects :as effects]
   [browser.theme :as theme]
   [redux.core :as f]
   [redux.chan-middleware :refer [chan-middleware]]
   [redux-map-action.core :as rm]))

(defn- t
  "Translate helper that uses current language from state"
  ([key] (i18n/t (:current-lang @st/state) key))
  ([key args] (i18n/t (:current-lang @st/state) key args)))

;; components

(defn BlogPostsTitleItem [post]
  ^{:key (g/id post)}
  [:li.BlogPostsTitleItem
   [:a {:className (classnames {:visited (get-in @st/state [:visited-posts (g/id post)])})
        :href (g/blog-url post)
        :aria-label (str (g/title post) ", " (g/date post))}
    [:time.BlogPostsTitleItem__date {:dateTime (g/date post)} (g/date post)]
    [:h3.BlogPostsTitleItem__title (g/title post)]]])

(defn BlogPosts []
  (let [loading? (:loading-post-list @st/state)]
    [:section.BlogPosts {:aria-label (t :post-list)
                         :aria-busy loading?}
     (if loading?
       [:p {:role "status" :aria-live "polite"} (t :loading)]
       [:ul {:role "list"}
        (->> (:posts @st/state)
             (map last)
             (sort-by #(g/date %) >)
             (map BlogPostsTitleItem)
             doall)])]))

(defn- format-date
  "Format ISO date string to readable format (YYYY-MM-DD)"
  [iso-str]
  (when iso-str
    (-> iso-str (subs 0 10))))

(defn- BlogPostMeta
  "Display post metadata: dates and tags"
  [post]
  (let [created (g/created-at post)
        updated (g/updated-at post)
        tags (g/post-tags post)
        dates-differ? (and created updated (not= (subs created 0 10) (subs updated 0 10)))]
    [:div.BlogPost__meta {:role "contentinfo" :aria-label (t :post-info)}
     (when created
       [:span.BlogPost__meta-dates
        [:time {:dateTime (format-date created)} (format-date created)]
        (when dates-differ?
          [:<>
           [:span.BlogPost__meta-arrow " → "]
           [:time {:dateTime (format-date updated)} (format-date updated)]])])
     (when (seq tags)
       [:ul.BlogPost__meta-tags {:aria-label (t :tags)}
        (for [tag tags]
          ^{:key tag}
          [:li.BlogPost__meta-tag
           [:a {:href (router/tag-url tag)
                :aria-label (t :view-tag-posts tag)}
            (str "#" tag)]])])]))

(defn BlogPost []
  (let [visiting-post-id (:visiting-post @st/state)
        visiting-post (get-in @st/state [:posts visiting-post-id])
        loading? (get-in @st/state [:loading-posts visiting-post-id])
        has-content? (boolean (:content visiting-post))
        is-loading? (or loading? (not has-content?))]
    [:article.BlogPost {:aria-busy is-loading?}
     [:header.BlogPost__header
      [:a.BlogPost__back-list {:href "#/"
                               :on-click #(router/go-back! % (:current-lang @st/state))
                               :aria-label (t :back)}
       [:svg.icon-back {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
                        :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
                        :aria-hidden "true" :focusable "false"}
        [:path {:d "M19 12H5"}]
        [:path {:d "M12 19l-7-7 7-7"}]]]
      [:h1 (g/title visiting-post)]
      [BlogPostMeta visiting-post]]
     (if is-loading?
       [:p {:role "status" :aria-live "polite"} (t :loading)]
       [:div.BlogPost__md
        {:dangerouslySetInnerHTML
         (r/unsafe-html (render-md (:content visiting-post)
                                   :heading-id-renderer #(g/heading-id visiting-post %)
                                   :lang (:post-lang visiting-post)))}])]))

(defn TagPosts []
  (let [tag (:visiting-tag @st/state)
        posts (:tag-posts @st/state)
        loading? (:loading-tag @st/state)]
    [:section.TagPosts {:aria-label (t :posts-tagged tag)}
     [:header.TagPosts__header
      [:a.TagPosts__back {:href "#/"
                          :on-click #(router/go-back! % (:current-lang @st/state))
                          :aria-label (t :back)}
       [:svg.icon-back {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
                        :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
                        :aria-hidden "true" :focusable "false"}
        [:path {:d "M19 12H5"}]
        [:path {:d "M12 19l-7-7 7-7"}]]]
      [:h1.TagPosts__title (str "#" tag)]]
     (if loading?
       [:p {:role "status" :aria-live "polite"} (t :loading)]
       [:ul.TagPosts__list {:role "list"}
        (->> posts
             (sort-by #(g/date %) >)
             (map BlogPostsTitleItem)
             doall)])]))

(defn FooterLinks
  "Display footer links: Language · GitHub · X · RSS
   position: :top-right (for BlogPosts) or :bottom-right (for BlogPost/TagPosts)"
  [position]
  (let [current-lang (:current-lang @st/state)
        lang-link (if (= current-lang :en) "#/" "#/en/")
        lang-text (if (= current-lang :en) "中文" "English")
        rss-url (str "https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/"
                     (if (= current-lang :en) "feed.en.xml" "feed.xml"))]
    [:nav.FooterLinks {:class (name position)}
     [:a {:href lang-link} lang-text]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href "https://github.com/bolasblack" :target "_blank" :rel "noopener"} "GitHub"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href "https://x.com/c4605" :target "_blank" :rel "noopener"} "X"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href rss-url :target "_blank" :rel "noopener"} "RSS"]]))

(defn App []
  [:<>
   [theme/ThemeToggle]
   [:main {:role "main"}
    (cond
      (:visiting-post @st/state)
      [:<>
       [BlogPost]
       [FooterLinks :bottom-right]]

      (:visiting-tag @st/state)
      [:<>
       [TagPosts]
       [FooterLinks :bottom-right]]

      :else
      [:<>
       [FooterLinks :top-right]
       [BlogPosts]])]])

;; initialize

(defn- create-store! []
  (let [devtools-enhancer (if js/window.__REDUX_DEVTOOLS_EXTENSION__
                            (rm/wrap-redux-devtools-enhancer
                             (js/window.__REDUX_DEVTOOLS_EXTENSION__
                              #js {:serialize
                                   #js {:replacer #(if (coll? %2)
                                                     #js {:cljs-struct true
                                                          :data (clj->js %2)}
                                                     %2)}}))
                            identity)
        enhancer (comp
                  (f/apply-middleware (chan-middleware effects/subscribe-dispatcher))
                  rm/enhancer
                  f/clj-atom-state-compatible-enhancer
                  devtools-enhancer)]
    (f/create-store st/reducer st/state enhancer)))

(defonce react-root (atom nil))

(defn ^:dev/before-load unmount-root []
  (when @react-root
    (rdomc/unmount @react-root)))

(defn ^:dev/after-load mount-root []
  (when-not @react-root
    (reset! react-root (rdomc/create-root (js/document.getElementById "app"))))
  (rdomc/render @react-root [App]))

(defonce ^:private initialized? (atom false))
(when-not @initialized?
  (reset! initialized? true)
  (dom-ready
   (fn []
     (theme/init-theme!)
     (create-store!)
     (mount-root))))
