(ns browser.core
  (:require
   [reagent.core :as r]
   [utils.async :as ua :include-macros true]
   [browser.utils :refer [dom-ready classnames]]
   [browser.flux :as f]
   [browser.github :as g]
   [redux-map-action.core :as rc]
   ["marked" :as md]))

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


(defmulti subscribe #(:type %))

(defmethod subscribe :posts-fetch [action store]
  (ua/go-try (f/dispatch! store {:type :posts-fetched
                                 :posts (ua/<? (g/get-posts))})))

(defmethod subscribe :post-show [{:keys [post]} store]
  (ua/go-try (f/dispatch! store {:type :post-fetch :post post})
             (f/dispatch! store {:type :post-fetched :post (ua/<? (g/get-post post))})))

(defmethod subscribe :default [])


;; components

(defn BlogPostsTitleItem [post]
  ^{:key (g/id post)}
  [:li.BlogPostsTitleItem {:on-click #(f/dispatch! @store {:type :post-show :post post})}
   [:a {:className (classnames {:visited (get-in @state [:visited-posts (g/id post)])})
        :href "javascript:;"}
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
      [:div.BlogPost__back-list
       {:on-click #(f/dispatch! @store {:type :post-unshow :post visiting-post})}
       [:i.icon-back]]
      [:h1 (g/title visiting-post)]]
     (if (and (get-in @state [:loading-posts visiting-post-id])
              (:content visiting-post))
       [:div "Loading..."]
       [:div.BlogPost__md {:dangerously-set-inner-HTML {:__html (md (:content visiting-post))}}])]))

(defn App []
  (if (:visiting-post @state)
    [BlogPost]
    [BlogPosts]))

;; initialize

(defn create-store! []
  (let [devtools-enhancer (if js/window.__REDUX_DEVTOOLS_EXTENSION__
                            (js/window.__REDUX_DEVTOOLS_EXTENSION__
                             #js {:serialize
                                  #js {:replacer #(if (coll? %2)
                                                    #js {:cljs-struct true
                                                         :data (clj->js %2)}
                                                    %2)}})
                            identity)
        enhancer (comp
                  rc/enhancer
                  f/clj-atom-state-compatible-enhancer
                  (f/apply-middleware (f/chan-middleware subscribe))
                  (rc/wrap-redux-devtools-enhancer devtools-enhancer))]
    (reset! store (f/create-store reducer state enhancer))
    (f/dispatch! @store {:type :posts-fetch})))

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
