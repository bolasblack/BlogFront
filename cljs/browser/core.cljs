(ns browser.core
  (:require
   [cljs.core.async :as a]
   [reagent.core :as r]
   [utils.async :as ua :include-macros true]
   [browser.utils :refer [dom-ready classnames render-md]]
   [browser.github :as g]
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
     visiting-post])

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

(defmethod reducer :post-unshow [state {:keys [post]}]
  (when (= (:visiting-post state) (g/id post))
    (assoc state :visiting-post nil)))

(defmethod reducer :default [state] state)


(defn subscribe-posts-fetch [action-chan res-chan]
  (ua/go-try
   (loop []
     (ua/<? (next-action action-chan :posts-fetch))
     (a/>! res-chan {:type :posts-fetched
                     :posts (ua/<? (g/get-posts))})
     (recur))))

(defn subscribe-post-show [action-chan res-chan]
  (ua/go-try
   (loop []
     (let [{:keys [post]} (ua/<? (next-action action-chan :post-show))]
       (a/>! res-chan {:type :post-fetch :post post})
       (let [post (if (:content post) post (ua/<? (g/get-post post)))]
         (a/>! res-chan {:type :post-fetched :post post})))
     (recur))))

(defn scroll-to-element-by-id [elem-id]
  (when-let [elem (js/document.getElementById elem-id)]
    (let [elem-rect (.getBoundingClientRect elem)
          scroll-top (+ js/document.documentElement.scrollTop elem-rect.top)]
      (js/scrollTo #js {:top scroll-top
                        :behavior "smooth"}))))

(defn subscribe-init-app [action-chan res-chan]
  (ua/go-try
   (a/>! res-chan {:type :posts-fetch})
   (ua/<? (next-action action-chan :posts-fetched))
   (when-let [url-info (g/parse-post-heading-id js/location.hash)]
     (when-let [post (get-in @state [:posts (:post-id url-info)])]
       (a/>! res-chan {:type :post-show :post post})
       (ua/<? (next-action action-chan :post-fetched))
       (ua/<? (a/timeout 0))
       (scroll-to-element-by-id (:heading-id url-info))))))

(defn subscribe-dispatcher [action-chan res-chan]
  (let [mult-action-chan (a/mult action-chan)]
    (doseq [subscribe
            [subscribe-init-app
             subscribe-posts-fetch
             subscribe-post-show]]
      (let [cloned-action-chan (a/tap mult-action-chan (a/chan))]
        ;; 当一个 subscriber 的 chan 结束以后必须 untap ，否则会阻塞
        ;; 主 action-chan 的 >! 函数
        (a/take!
         (subscribe cloned-action-chan res-chan)
         #(a/untap mult-action-chan cloned-action-chan))))))

;; components

(defn BlogPostsTitleItem [post]
  ^{:key (g/id post)}
  [:li.BlogPostsTitleItem {:on-click #(f/dispatch! @store {:type :post-show :post post})}
   [:a {:className (classnames {:visited (get-in @state [:visited-posts (g/id post)])})
        :href (g/blog-url post)}
    [:time.BlogPostsTitleItem__date (g/date post)]
    [:h3.BlogPostsTitleItem__title (g/title post)]]])

(defn BlogPosts []
  [:div.BlogPosts
   (if (:loading-post-list @state)
     [:h1 "Loading..."]
     [:ul (->> (:posts @state)
               (map last)
               (sort-by #(g/date %) >)
               (map BlogPostsTitleItem)
               doall)])])

(defn BlogPost []
  (let [visiting-post-id (:visiting-post @state)
        visiting-post (get-in @state [:posts visiting-post-id])]
    [:article.BlogPost
     [:header.BlogPost__header
      [:a.BlogPost__back-list
       {:href "#/"
        :on-click #(f/dispatch! @store {:type :post-unshow :post visiting-post})}
       [:i.icon-back]]
      [:h1 (g/title visiting-post)]]
     (if (or (get-in @state [:loading-posts visiting-post-id])
             (not (:content visiting-post)))
       [:div "Loading..."]
       [:div.BlogPost__md
        {:dangerously-set-inner-HTML
         {:__html (render-md (:content visiting-post)
                             :heading-id-renderer #(g/heading-id visiting-post %))}}])]))

(defn App []
  (if (:visiting-post @state)
    [BlogPost]
    [BlogPosts]))

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

(defn ^:dev/before-load unmount-root []
  (r/unmount-component-at-node
   (js/document.getElementById "app")))

(defn ^:dev/after-load mount-root []
  (r/render
   [App]
   (js/document.getElementById "app")))

(if-not @store
  (dom-ready
   (fn []
     (create-store!)
     (mount-root))))
