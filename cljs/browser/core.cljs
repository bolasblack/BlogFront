(ns browser.core
  (:require
   [cljs.core.async :as a]
   [reagent.core :as r]
   [reagent.dom.client :as rdomc]
   [rxcljs.core :as rc :include-macros true]
   [browser.utils :refer [dom-ready classnames render-md]]
   [browser.github :as g]
   [browser.theme :as theme]
   [redux.core :as f]
   [redux.chan-middleware :refer [chan-middleware next-action]]
   [redux-map-action.core :as rm]))

;; store

(defonce store (atom nil))

(defrecord State
    [loading-post-list
     posts
     loading-posts
     visited-posts
     visiting-post
     ;; Tag view state
     visiting-tag
     tag-posts
     loading-tag])

(defonce state
  (r/atom (map->State {})))


(defmulti reducer #(:type %2))

(defmethod reducer :posts-fetch [state action]
  (assoc state :loading-post-list true))

(defmethod reducer :posts-fetched [state {:keys [posts]}]
  (let [ziped-posts (zipmap (map g/id posts) posts)]
    (-> state
        (assoc :loading-post-list false)
        (assoc :posts ziped-posts))))

(defmethod reducer :post-fetch [state {:keys [post]}]
  (assoc-in state [:loading-posts (g/id post)] true))

(defmethod reducer :post-fetched [state {:keys [post]}]
  (-> state
      (assoc-in [:posts (g/id post)] post)
      (assoc-in [:loading-posts (g/id post)] false)))

(defmethod reducer :post-show [state {:keys [post]}]
  (-> state
      (assoc-in [:visited-posts (g/id post)] true)
      (assoc-in [:visiting-post] (g/id post))))

(defmethod reducer :post-unshow [state {:keys [post-id]}]
  (if (= (:visiting-post state) post-id)
    (assoc state :visiting-post nil)
    state))

;; Tag reducers
(defmethod reducer :tag-show [state {:keys [tag]}]
  (-> state
      (assoc :visiting-tag tag)
      (assoc :loading-tag true)))

(defmethod reducer :tag-fetched [state {:keys [tag posts]}]
  (-> state
      (assoc :tag-posts posts)
      (assoc :loading-tag false)))

(defmethod reducer :tag-unshow [state _]
  (-> state
      (assoc :visiting-tag nil)
      (assoc :tag-posts nil)))

(defmethod reducer :default [state] state)


(defn subscribe-posts-fetch [action-chan res-chan]
  (rc/go-loop []
    (rc/<! (next-action action-chan :posts-fetch))
    (rc/>! res-chan {:type :posts-fetched
                     :posts (rc/<! (g/get-posts))})
    (recur)))

(defn subscribe-post-show [action-chan res-chan]
  (rc/go-loop []
    (let [{:keys [post]} (rc/<! (next-action action-chan :post-show))]
      (rc/>! res-chan {:type :post-fetch :post post})
      (let [post (if (:content post) post (rc/<! (g/get-post post)))]
        (rc/>! res-chan {:type :post-fetched :post post})))
    (recur)))

(defn subscribe-tag-show [action-chan res-chan]
  (rc/go-loop []
    (let [{:keys [tag]} (rc/<! (next-action action-chan :tag-show))
          tag-data (rc/<! (g/get-tag-posts tag))]
      (rc/>! res-chan {:type :tag-fetched
                       :tag (:tag tag-data)
                       :posts (:posts tag-data)}))
    (recur)))

(defn scroll-to-element-by-id [elem-id]
  (when-let [elem (js/document.getElementById elem-id)]
    (let [elem-rect (.getBoundingClientRect elem)
          scroll-top (+ js/document.scrollingElement.scrollTop elem-rect.top)]
      (js/scrollTo #js {:top scroll-top
                        :behavior "smooth"}))))

(defn on-url-hash-changed [action-chan res-chan hash]
  (rc/go
    (let [route (g/parse-url-hash hash)]
      (case (:type route)
        :post
        (when-let [post (get-in @state [:posts (:post-id route)])]
          ;; Clear any tag view first
          (when (:visiting-tag @state)
            (rc/>! res-chan {:type :tag-unshow}))
          (rc/>! res-chan {:type :post-show :post post})
          (rc/<! (next-action action-chan :post-fetched))
          (rc/<! (a/timeout 0))
          (scroll-to-element-by-id (:heading-id route)))

        :tag
        (do
          ;; Clear any post view first
          (when-let [visiting-post (:visiting-post @state)]
            (rc/>! res-chan {:type :post-unshow :post-id visiting-post}))
          (rc/>! res-chan {:type :tag-show :tag (:tag route)}))

        :home
        (do
          (when-let [visiting-post (:visiting-post @state)]
            (rc/>! res-chan {:type :post-unshow :post-id visiting-post}))
          (when (:visiting-tag @state)
            (rc/>! res-chan {:type :tag-unshow})))))))

(defn subscribe-init-app [action-chan res-chan]
  (rc/go
    (rc/>! res-chan {:type :posts-fetch})
    (rc/<! (next-action action-chan :posts-fetched))
    (rc/<! (on-url-hash-changed action-chan res-chan js/location.hash))
    (js/window.addEventListener
     "hashchange"
     #(on-url-hash-changed action-chan res-chan js/location.hash))))

(defn subscribe-dispatcher [action-chan res-chan]
  (let [mult-action-chan (a/mult action-chan)]
    (doseq [subscribe
            [subscribe-init-app
             subscribe-posts-fetch
             subscribe-post-show
             subscribe-tag-show]]
      (let [cloned-action-chan (a/tap mult-action-chan (a/chan (a/sliding-buffer 1)))]
        (subscribe cloned-action-chan res-chan)))))

;; components

(defn BlogPostsTitleItem [post]
  ^{:key (g/id post)}
  [:li.BlogPostsTitleItem
   [:a {:className (classnames {:visited (get-in @state [:visited-posts (g/id post)])})
        :href (g/blog-url post)
        :aria-label (str (g/title post) ", " (g/date post))}
    [:time.BlogPostsTitleItem__date {:dateTime (g/date post)} (g/date post)]
    [:h3.BlogPostsTitleItem__title (g/title post)]]])

(defn BlogPosts []
  (let [loading? (:loading-post-list @state)]
    [:section.BlogPosts {:aria-label "文章列表"
                         :aria-busy loading?}
     (if loading?
       [:p {:role "status" :aria-live "polite"} "Loading..."]
       [:ul {:role "list"}
        (->> (:posts @state)
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
    [:div.BlogPost__meta {:role "contentinfo" :aria-label "文章信息"}
     (when created
       [:span.BlogPost__meta-dates
        [:time.BlogPost__meta-created {:dateTime (format-date created)}
         (str "创建于 " (format-date created))]
        (when dates-differ?
          [:time.BlogPost__meta-updated {:dateTime (format-date updated)}
           (str " · 更新于 " (format-date updated))])])
     (when (seq tags)
       [:ul.BlogPost__meta-tags {:aria-label "标签"}
        (for [tag tags]
          ^{:key tag}
          [:li.BlogPost__meta-tag
           [:a {:href (g/tag-url tag)
                :aria-label (str "查看标签 " tag " 的所有文章")}
            (str "#" tag)]])])]))

(defn BlogPost []
  (let [visiting-post-id (:visiting-post @state)
        visiting-post (get-in @state [:posts visiting-post-id])
        loading? (get-in @state [:loading-posts visiting-post-id])
        has-content? (boolean (:content visiting-post))
        is-loading? (or loading? (not has-content?))]
    [:article.BlogPost {:aria-busy is-loading?}
     [:header.BlogPost__header
      [:a.BlogPost__back-list {:href "#/"
                               :aria-label "返回文章列表"}
       [:i.icon-back {:aria-hidden "true"}]]
      [:h1 (g/title visiting-post)]
      [BlogPostMeta visiting-post]]
     (if is-loading?
       [:p {:role "status" :aria-live "polite"} "Loading..."]
       [:div.BlogPost__md
        {:dangerouslySetInnerHTML
         (r/unsafe-html (render-md (:content visiting-post)
                                   :heading-id-renderer #(g/heading-id visiting-post %)))}])]))

(defn TagPosts []
  (let [tag (:visiting-tag @state)
        posts (:tag-posts @state)
        loading? (:loading-tag @state)]
    [:section.TagPosts {:aria-label (str "标签 " tag " 的文章")}
     [:header.TagPosts__header
      [:a.TagPosts__back {:href "#/"
                          :aria-label "返回文章列表"}
       [:i.icon-back {:aria-hidden "true"}]]
      [:h1.TagPosts__title (str "#" tag)]]
     (if loading?
       [:p {:role "status" :aria-live "polite"} "Loading..."]
       [:ul.TagPosts__list {:role "list"}
        (->> posts
             (sort-by #(g/date %) >)
             (map BlogPostsTitleItem)
             doall)])]))

(defn App []
  [:<>
   [theme/ThemeToggle]
   [:main {:role "main"}
    (cond
      (:visiting-post @state) [BlogPost]
      (:visiting-tag @state) [TagPosts]
      :else [BlogPosts])]])

;; initialize

(defn create-store! []
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
                  (f/apply-middleware (chan-middleware subscribe-dispatcher))
                  rm/enhancer
                  f/clj-atom-state-compatible-enhancer
                  devtools-enhancer)]
    (reset! store (f/create-store reducer state enhancer))))

(defonce react-root (atom nil))

(defn ^:dev/before-load unmount-root []
  (when @react-root
    (rdomc/unmount @react-root)))

(defn ^:dev/after-load mount-root []
  (when-not @react-root
    (reset! react-root (rdomc/create-root (js/document.getElementById "app"))))
  (rdomc/render @react-root [App]))

(if-not @store
  (dom-ready
   (fn []
     (theme/init-theme!)
     (create-store!)
     (mount-root))))
