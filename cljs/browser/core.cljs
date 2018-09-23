(ns browser.core
  (:require
   [reagent.core :as r]
   [utils.async :as ua :include-macros true]
   [browser.utils :refer [dom-ready]]
   [browser.flux :as f]
   [browser.github :as g]
   [redux-map-action.core :as rc]))

;; state

(defrecord State
    [loading-posts
     posts
     loading-post])

(defonce state
  (r/atom (map->State {})))


(defmulti reducer #(:type %2))

(defmethod reducer :posts-fetch [state action]
  (assoc state :loading-posts true))

(defmethod reducer :posts-fetched [state {:keys [posts]}]
  (let [ziped-posts (zipmap (map g/id posts) posts)]
    (-> state
        (assoc :loading-posts false)
        (assoc :posts ziped-posts))))

(defmethod reducer :post-fetch [state {:keys [post]}]
  (assoc-in state [:loading-post (g/id post)] true))

(defmethod reducer :post-fetched [state {:keys [post]}]
  (-> state
      (assoc-in [:loading-post (g/id post)] false)
      (assoc-in [:posts (g/id post)] post)))

(defmethod reducer :default [state] state)


(defmulti subscribe #(:type %))

(defmethod subscribe :posts-fetch [action store]
  (ua/go-try (f/dispatch! store {:type :posts-fetched
                                 :posts (ua/<? (g/get-posts))})))

(defmethod subscribe :post-title-clicked [{:keys [post]} store]
  (ua/go-try (f/dispatch! store {:type :post-fetch :post post})
             (f/dispatch! store {:type :post-fetched :post (ua/<? (g/get-post post))})))

(defmethod subscribe :default [])


;; components

(defn blog-post-title-item [post]
  ^{:key (g/id post)}
  [:li.blog-post {:on-click #(f/dispatch! @store {:type :post-title-clicked :post post})}
   [:time (g/date post)]
   [:h3 (g/title post)]])

(defn blog-posts []
  [:div.blog-posts
   (if (:loading-posts @state)
     [:h1 "Loading..."]
     [:ul (map #(blog-post-title-item (last %)) (:posts @state))])])

(defn app []
  [blog-posts])

;; initialize

(defonce store (atom nil))

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
   [app]
   (js/document.getElementById "app")))

(if-not @store
  (dom-ready
   (fn []
     (create-store!)
     (mount-root))))
