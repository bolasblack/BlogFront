(ns browser.core
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [reagent.dom.client :as rdomc]
   [browser.utils :refer [dom-ready classnames render-md]]
   [browser.router :as router]
   [browser.state :as st]
   [browser.state-effects :as effects]
   [browser.theme :as theme]
   [redux.core :as f]
   [redux.chan-middleware :refer [chan-middleware]]
   [redux-map-action.core :as rm]
   [shared.constants :refer [preload-state-html-id]]
   [shared.components :as ui]
   [shared.router :as shared-router]
   [shared.state :as shared-state]))

;; ============================================================================
;; Smart Components (stateful wrappers around shared dumb components)
;; ============================================================================

(defn BlogPosts$
  "Smart wrapper: reads posts from state"
  []
  (let [loading? (boolean (:loading-post-list @st/state))
        lang (:current-lang @st/state)
        posts (->> (:posts @st/state) vals)]
    [ui/BlogPosts
     {:posts posts
      :lang lang
      :loading? loading?
      :item-props-fn (fn [post]
                       (let [href (shared-router/blog-url (:id post) (:lang post))]
                         {:href href
                          :class-fn #(classnames {:visited (get-in @st/state [:visited-posts (:id post)])})
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (router/navigate! href))}))}]))

(defn BlogPost$
  "Smart wrapper: reads current post from state"
  []
  (let [visiting-post (:visiting-post @st/state)
        visiting-post-id (:id visiting-post)
        loading? (get-in @st/state [:loading-posts visiting-post-id])
        has-content? (not (str/blank? (:content visiting-post)))
        is-loading? (or loading? (not has-content?))
        lang (:current-lang @st/state)]
    [ui/BlogPost
     {:post {:title (when visiting-post (:title visiting-post))
             :created-at (when visiting-post (:created-at visiting-post))
             :updated-at (when visiting-post (:updated-at visiting-post))
             :tags (when visiting-post (:tags visiting-post))}
      :lang lang
      :loading? is-loading?
      :back-on-click #(router/go-back! % lang)
      :content [:div.BlogPost__md
                {:dangerouslySetInnerHTML
                 (r/unsafe-html (render-md (:content visiting-post)
                                           :heading-id-renderer (when visiting-post
                                                                  #(shared-router/heading-url (:id visiting-post) % (:lang visiting-post)))
                                           :lang (:post-lang visiting-post)))}]}]))

(defn TagPosts$
  "Smart wrapper: reads tag posts from state"
  []
  (let [tag (:visiting-tag @st/state)
        posts (:tag-posts @st/state)
        loading? (boolean (:loading-tag-post-list @st/state))
        lang (:current-lang @st/state)]
    [ui/TagPosts
     {:tag tag
      :posts posts
      :lang lang
      :loading? loading?
      :back-on-click #(router/go-back! % lang)
      :item-props-fn (fn [post]
                       (let [href (shared-router/blog-url (:id post) (:lang post))]
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
       [FooterLinks$ :bottom-center]]

      (:visiting-tag @st/state)
      [:<>
       [TagPosts$]
       [FooterLinks$ :bottom-center]]

      :else
      [:<>
       [BlogPosts$]
       [FooterLinks$ :bottom-center]])]])

;; ============================================================================
;; Initialization
;; ============================================================================

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

(defn- has-ssr-content?
  "Check if the app element has SSR-rendered content"
  []
  (when-let [app-el (js/document.getElementById "app")]
    (let [inner (.-innerHTML app-el)]
      (and inner (not (str/blank? inner))))))

(defn- parse-ssr-state
  "Parse SSR state from script tag if present"
  []
  (when-let [script-el (js/document.getElementById preload-state-html-id)]
    (try
      (let [json-str (.-textContent script-el)
            js-data (js/JSON.parse json-str)
            state-data (shared-state/js->state js-data)]
        state-data)
      (catch js/Error _
        nil))))

(defn- init-state-for-hydration!
  "Initialize state from SSR data before hydration to match SSR content.
   This ensures the client renders the same view as SSR."
  []
  (when-let [ssr-state (parse-ssr-state)]
    (reset! st/state ssr-state)))

(defn unmount-root []
  (when @react-root
    (rdomc/unmount @react-root)))

(defn mount-root []
  (let [app-el (js/document.getElementById "app")]
    (when-not @react-root
      ;; Use hydrateRoot for SSR content, createRoot for fresh render
      (reset! react-root
              (if (has-ssr-content?)
                (rdomc/hydrate-root app-el [App])
                (rdomc/create-root app-el))))
    ;; Only call render for createRoot (hydrate-root doesn't need it)
    (when-not (has-ssr-content?)
      (rdomc/render @react-root [App]))))

(defn ^:dev/after-load remount-root []
  (rdomc/render @react-root [App]))

(defonce ^:private initialized? (atom false))
(when-not @initialized?
  (reset! initialized? true)
  (dom-ready
   (fn []
     (theme/init-theme!)
     ;; Initialize state from URL before hydration to match SSR
     (when (has-ssr-content?)
       (init-state-for-hydration!))
     (create-store!)
     (mount-root))))
