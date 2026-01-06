(ns shared.components
  "Shared presentational (dumb) components for both browser and worker.
   These components receive all data as props and contain no state logic."
  (:require
   [shared.i18n :as i18n]
   [shared.router :as router]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- t [lang key & [args]]
  (i18n/t lang key args))

(defn- format-date
  "Format ISO date string to YYYY-MM-DD"
  [iso-str]
  (when iso-str (subs iso-str 0 10)))

;; ============================================================================
;; Presentational Components
;; ============================================================================

(defn BlogPostsTitleItem
  "Render a single post title item.
   Props: {:post map :lang keyword :href string :class-fn fn :on-click fn}"
  [{:keys [post lang href class-fn on-click]}]
  (let [date (:date post)
        title (:title post)
        id (:id post)]
    [:li.BlogPostsTitleItem
     [:a (cond-> {:href (or href (str (router/lang-prefix lang) id))}
           class-fn (assoc :className (class-fn))
           on-click (assoc :on-click on-click)
           true (assoc :aria-label (str title ", " date)))
      [:time.BlogPostsTitleItem__date {:dateTime date} date]
      [:h3.BlogPostsTitleItem__title title]]]))

(defn BlogPosts
  "Render the posts list.
   Props: {:posts seq :lang keyword :loading? boolean :item-props-fn fn}"
  [{:keys [posts lang loading? item-props-fn]}]
  [:section.BlogPosts {:aria-label (t lang :ui/post-list)
                       :aria-busy loading?}
   (if loading?
     [:p {:role "status" :aria-live "polite"} (t lang :ui/loading)]
     [:ul {:role "list"}
      (->> posts
           (sort-by :date >)
           (map (fn [post]
                  (let [base-props {:post post :lang lang}
                        extra-props (when item-props-fn (item-props-fn post))]
                    ^{:key (:id post)}
                    [BlogPostsTitleItem (merge base-props extra-props)])))
           doall)])])

(defn BlogPostMeta
  "Render post metadata (dates and tags).
   Props: {:post map :lang keyword :tag-aria-label-fn fn}"
  [{:keys [post lang tag-aria-label-fn]}]
  (let [created (or (:created-at post) (:post-created-at post))
        updated (or (:updated-at post) (:post-updated-at post))
        tags (or (:tags post) (:post-tags post))
        dates-differ? (and created updated (not= (subs created 0 10) (subs updated 0 10)))]
    [:div.BlogPost__meta {:role "contentinfo" :aria-label (t lang :ui/post-info)}
     (when created
       [:span.BlogPost__meta-dates
        [:time {:dateTime (format-date created)} (format-date created)]
        (when dates-differ?
          [:<>
           [:span.BlogPost__meta-arrow " → "]
           [:time {:dateTime (format-date updated)} (format-date updated)]])])
     (when (seq tags)
       [:ul.BlogPost__meta-tags {:aria-label (t lang :ui/tags)}
        (for [tag tags]
          ^{:key tag}
          [:li.BlogPost__meta-tag
           [:a (cond-> {:href (router/tag-url tag lang)}
                 tag-aria-label-fn (assoc :aria-label (tag-aria-label-fn tag)))
            (str "#" tag)]])])]))

(defn BackButton
  "Render the back button with SVG icon.
   Props: {:href string :on-click fn :aria-label string :with-stroke? boolean :class string}"
  [{:keys [href on-click aria-label with-stroke? class]}]
  [(keyword (str "a" (or class ".BlogPost__back-list"))) (cond-> {:href href}
                            on-click (assoc :on-click on-click)
                            aria-label (assoc :aria-label aria-label))
   [:svg.icon-back (cond-> {:width "18" :height "18" :viewBox "0 0 24 24"}
                     with-stroke? (merge {:fill "none"
                                          :stroke "currentColor"
                                          :stroke-width "2"
                                          :stroke-linecap "round"
                                          :aria-hidden "true"
                                          :focusable "false"}))
    [:path {:d "M19 12H5"}]
    [:path {:d "M12 19l-7-7 7-7"}]]])

(defn BlogPost
  "Render a blog post article.
   Props: {:post map :lang keyword :loading? boolean :content hiccup
           :back-href string :back-on-click fn}"
  [{:keys [post lang loading? content back-href back-on-click]}]
  (let [title (or (:title post) (:post-title post))]
    [:article.BlogPost {:aria-busy loading?}
     [:header.BlogPost__header
      [BackButton {:href (or back-href (router/home-url lang))
                   :on-click back-on-click
                   :aria-label (t lang :ui/back)
                   :with-stroke? (some? back-on-click)}]
      [:h1 title]
      [BlogPostMeta {:post post :lang lang :tag-aria-label-fn #(t lang :ui/view-tag-posts %)}]]
     (if loading?
       [:p {:role "status" :aria-live "polite"} (t lang :ui/loading)]
       content)]))

(defn TagPosts
  "Render tag posts page.
   Props: {:tag string :posts seq :lang keyword :loading? boolean
           :back-on-click fn :item-props-fn fn}"
  [{:keys [tag posts lang loading? back-on-click item-props-fn]}]
  [:section.TagPosts {:aria-label (t lang :ui/posts-tagged tag)}
   [:header.TagPosts__header
    [BackButton {:href (router/home-url lang)
                 :on-click back-on-click
                 :aria-label (t lang :ui/back)
                 :with-stroke? (some? back-on-click)
                 :class ".TagPosts__back"}]
    [:h1.TagPosts__title (str "#" tag)]]
   (if loading?
     [:p {:role "status" :aria-live "polite"} (t lang :ui/loading)]
     [:ul.TagPosts__list {:role "list"}
      (->> posts
           (sort-by :date >)
           (map (fn [post]
                  (let [base-props {:post post :lang lang}
                        extra-props (when item-props-fn (item-props-fn post))]
                    ^{:key (:id post)}
                    [BlogPostsTitleItem (merge base-props extra-props)])))
           doall)])])

(defn FooterLinks
  "Render footer navigation links.
   Props: {:lang keyword :position keyword}"
  [{:keys [lang position]}]
  (let [lang-link (if (= lang :en) "/" "/en/")
        lang-text (if (= lang :en) "中文" "English")
        rss-url (str "https://raw.githubusercontent.com/bolasblack/BlogPosts/master/_meta/"
                     (if (= lang :en) "feed.en.xml" "feed.xml"))]
    [:nav.FooterLinks {:class (when position (name position))}
     [:a {:href lang-link} lang-text]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href "https://github.com/bolasblack" :target "_blank" :rel "noopener"} "GitHub"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href "https://x.com/c4605" :target "_blank" :rel "noopener"} "X"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href rss-url :target "_blank" :rel "noopener"} "RSS"]]))

(defn ThemeToggle
  "Render theme toggle button (placeholder for SSR).
   Props: {:lang keyword :on-click fn}"
  [{:keys [lang on-click]}]
  [:button.ThemeToggle (cond-> {:aria-label (t lang :theme/switch-to-auto)}
                         on-click (assoc :on-click on-click
                                         :type "button"))])
