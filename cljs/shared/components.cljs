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
        id (:id post)
        class-name (when class-fn (class-fn))]
    [:li.BlogPostsTitleItem
     [:a (cond-> {:href (or href (router/blog-url id lang))
                  :aria-label (str title ", " date)
                  :on-click on-click}
           ;; Only add className if it's non-empty to match SSR output
           (and class-name (seq class-name)) (assoc :className class-name))
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
   Props: {:href string :on-click fn :aria-label string :class string}"
  [{:keys [href on-click aria-label class]}]
  [(keyword (str "a" (or class ".BlogPost__back-list"))) (cond-> {:href href}
                                                           on-click (assoc :on-click on-click)
                                                           aria-label (assoc :aria-label aria-label))
   ;; Always use consistent SVG attributes for SSR hydration compatibility
   [:svg.icon-back {:width "18" :height "18" :viewBox "0 0 24 24"
                    :fill "none"
                    :stroke "currentColor"
                    :stroke-width "2"
                    :stroke-linecap "round"
                    :aria-hidden "true"
                    :focusable "false"}
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
                   :aria-label (t lang :ui/back)}]
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
     [:a.FooterLinks__external {:href "https://github.com/bolasblack/BlogFront" :target "_blank" :rel "noopener"} "Source Code"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href "https://x.com/c4605" :target "_blank" :rel "noopener"} "X"]
     [:span.FooterLinks__sep "·"]
     [:a.FooterLinks__external {:href rss-url :target "_blank" :rel "noopener"} "RSS"]]))

(defn ThemeToggle
  "Render theme toggle button.
   For SSR, renders the initial state (system theme).
   For browser, the browser/theme.cljs component should be used instead.
   Props: {:lang keyword :on-click fn :theme keyword :next-label string :current-label string}"
  [{:keys [lang on-click next-label current-label]}]
  (let [next-label (or next-label (t lang :theme/switch-to-light))
        current-label (or current-label (t lang :theme/auto))]
    [:button.ThemeToggle (cond-> {:type "button"
                                  :aria-label next-label
                                  :on-click on-click}
                           current-label (assoc :title (t lang :theme/current current-label)))
     [:span.ThemeToggle__icon {:aria-hidden "true"}
      ;; Sun icon (shown when dark, clicks to light)
      [:svg.ThemeToggle__sun
       {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
        :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
        :focusable "false"}
       [:circle {:cx "12" :cy "12" :r "5"}]
       [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
       [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
       [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
       [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
       [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
       [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
       [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
       [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]
      ;; Moon icon (shown when light, clicks to dark)
      [:svg.ThemeToggle__moon
       {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
        :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
        :focusable "false"}
       [:path {:d "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"}]]
      ;; Auto icon (shown when system, indicates auto mode)
      [:svg.ThemeToggle__auto
       {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
        :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
        :focusable "false"}
       [:circle {:cx "12" :cy "12" :r "9"}]
       [:path {:d "M12 3v18"}]
       [:path {:d "M12 3a9 9 0 0 1 0 18" :fill "currentColor" :stroke "none"}]]]
     [:span.ThemeToggle__label {:aria-hidden "true"} current-label]]))
