(ns browser.state-effects
  (:require
   [cljs.core.async :as a]
   [rxcljs.core :as rc :include-macros true]
   [browser.github :as g]
   [browser.router :as nav]
   [browser.state :as st]
   [browser.utils :refer [scroll-to-element-by-id]]
   [redux.chan-middleware :refer [next-action]]
   [shared.github :as gh]
   [shared.router :as router]))

(defn subscribe-posts-fetch [action-chan res-chan]
  (rc/go-loop []
    (let [{:keys [lang]} (rc/<! (next-action action-chan :posts-fetch))]
      (rc/>! res-chan {:type :posts-fetched
                       :posts (rc/<! (g/get-posts lang))}))
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
    (let [{:keys [tag lang]} (rc/<! (next-action action-chan :tag-show))
          tag-data (rc/<! (g/get-tag-posts tag lang))]
      (rc/>! res-chan {:type :tag-fetched
                       :posts (:posts tag-data)}))
    (recur)))

(defn on-route-changed [action-chan res-chan]
  (rc/go
    (let [route (router/parse-pathname js/location.pathname js/location.hash)
          lang (:lang route)
          lang-changed? (not= lang (:current-lang @st/state))]
      ;; Update language if changed
      (when lang-changed?
        (rc/>! res-chan {:type :lang-changed :lang lang})
        ;; Reload posts for new language
        (rc/>! res-chan {:type :posts-fetch :lang lang})
        (rc/<! (next-action action-chan :posts-fetched)))
      (case (:type route)
        :goto-article
        ;; Find post by md-url and redirect to the correct article URL
        (when-let [post (gh/find-post-by-md-url (vals (:posts @st/state)) (:md-url route))]
          (nav/navigate! (g/blog-url post)))

        :post
        (when-let [post (get-in @st/state [:posts (:id route)])]
          ;; Clear any tag view first
          (when (:visiting-tag @st/state)
            (rc/>! res-chan {:type :tag-unshow}))
          (rc/>! res-chan {:type :post-show :post post})
          (rc/<! (next-action action-chan :post-fetched))
          (rc/<! (a/timeout 0))
          (when (:heading route)
            (scroll-to-element-by-id (:heading route))))

        :tag
        (do
          ;; Clear any post view first
          (when-let [visiting-post (:visiting-post @st/state)]
            (rc/>! res-chan {:type :post-unshow :post-id visiting-post}))
          (rc/>! res-chan {:type :tag-show :tag (:id route) :lang lang}))

        :home
        (do
          (when-let [visiting-post (:visiting-post @st/state)]
            (rc/>! res-chan {:type :post-unshow :post-id visiting-post}))
          (when (:visiting-tag @st/state)
            (rc/>! res-chan {:type :tag-unshow})))))))

(defn- redirect-legacy-hash!
  "Check for legacy hash-based routes and redirect to new path-based URLs.
   Returns true if redirect happened, false otherwise."
  []
  (when-let [new-url (router/parse-legacy-hash js/location.hash :zh)]
    ;; Replace current history entry with the new URL (no back to hash route)
    (js/history.replaceState #js {} "" new-url)
    true))

(defn subscribe-init-app [action-chan res-chan]
  (rc/go
    ;; Check and redirect legacy hash routes before loading
    (redirect-legacy-hash!)
    (rc/>! res-chan {:type :posts-fetch})
    (rc/<! (next-action action-chan :posts-fetched))
    (nav/update-nav-depth!)
    (rc/<! (on-route-changed action-chan res-chan))
    ;; Use popstate for path-based routing (History API)
    (js/window.addEventListener
     "popstate"
     (fn []
       (nav/update-nav-depth!)
       (on-route-changed action-chan res-chan)))))

(defn subscribe-dispatcher [action-chan res-chan]
  (let [mult-action-chan (a/mult action-chan)]
    (doseq [subscribe
            [subscribe-init-app
             subscribe-posts-fetch
             subscribe-post-show
             subscribe-tag-show]]
      (let [cloned-action-chan (a/tap mult-action-chan (a/chan (a/sliding-buffer 1)))]
        (subscribe cloned-action-chan res-chan)))))
