(ns browser.core
  (:require
   [reagent.core :as r]
   [reagent.dom.client :as rdomc]
   [browser.utils :refer [dom-ready classnames render-md]]
   [browser.github :as g]
   [browser.router :as router]
   [browser.state :as st]
   [browser.state-effects :as effects]
   [browser.theme :as theme]
   [redux.core :as f]
   [redux.chan-middleware :refer [chan-middleware]]
   [redux-map-action.core :as rm]
   [shared.components :as ui]))

;; ============================================================================
;; Smart Components (stateful wrappers around shared dumb components)
;; ============================================================================

(defn BlogPosts$
  "Smart wrapper: reads posts from state"
  []
  (let [loading? (:loading-post-list @st/state)
        lang (:current-lang @st/state)
        posts (->> (:posts @st/state) (map last))]
    [ui/BlogPosts
     {:posts (map (fn [p] {:id (g/id p) :date (g/date p) :title (g/title p) :post-lang (:post-lang p)
                           :_post p}) posts)
      :lang lang
      :loading? loading?
      :item-props-fn (fn [post]
                       (let [href (g/blog-url (:_post post))]
                         {:href href
                          :class-fn #(classnames {:visited (get-in @st/state [:visited-posts (:id post)])})
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (router/navigate! href))}))}]))

(defn BlogPost$
  "Smart wrapper: reads current post from state"
  []
  (let [visiting-post-id (:visiting-post @st/state)
        visiting-post (get-in @st/state [:posts visiting-post-id])
        loading? (get-in @st/state [:loading-posts visiting-post-id])
        has-content? (boolean (:content visiting-post))
        is-loading? (or loading? (not has-content?))
        lang (:current-lang @st/state)]
    [ui/BlogPost
     {:post {:title (g/title visiting-post)
             :created-at (g/created-at visiting-post)
             :updated-at (g/updated-at visiting-post)
             :tags (g/post-tags visiting-post)}
      :lang lang
      :loading? is-loading?
      :back-on-click #(router/go-back! % lang)
      :content [:div.BlogPost__md
                {:dangerouslySetInnerHTML
                 (r/unsafe-html (render-md (:content visiting-post)
                                           :heading-id-renderer #(g/heading-id visiting-post %)
                                           :lang (:post-lang visiting-post)))}]}]))

(defn TagPosts$
  "Smart wrapper: reads tag posts from state"
  []
  (let [tag (:visiting-tag @st/state)
        posts (:tag-posts @st/state)
        loading? (:loading-tag @st/state)
        lang (:current-lang @st/state)]
    [ui/TagPosts
     {:tag tag
      :posts (map (fn [p] {:id (g/id p) :date (g/date p) :title (g/title p) :_post p}) posts)
      :lang lang
      :loading? loading?
      :back-on-click #(router/go-back! % lang)
      :item-props-fn (fn [post]
                       (let [href (g/blog-url (:_post post))]
                         {:href href
                          :class-fn #(classnames {:visited (get-in @st/state [:visited-posts (:id post)])})
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (router/navigate! href))}))}]))

(defn FooterLinks$
  "Smart wrapper: reads lang from state"
  [position]
  [ui/FooterLinks {:lang (:current-lang @st/state) :position position}])

(defn App []
  [:<>
   [theme/ThemeToggle]
   [:main {:role "main"}
    (cond
      (:visiting-post @st/state)
      [:<>
       [BlogPost$]
       [FooterLinks$ :bottom-right]]

      (:visiting-tag @st/state)
      [:<>
       [TagPosts$]
       [FooterLinks$ :bottom-right]]

      :else
      [:<>
       [FooterLinks$ :top-right]
       [BlogPosts$]])]])

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
