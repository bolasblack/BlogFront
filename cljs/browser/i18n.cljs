(ns browser.i18n
  (:require [goog.string :as gstr]
            [goog.string.format]))

(def ^:private translations
  {;; Post list
   :post-list        {:zh "文章列表" :en "Post list"}
   :post-info        {:zh "文章信息" :en "Post info"}
   :tags             {:zh "标签" :en "Tags"}
   :view-tag-posts   {:zh "查看标签 %s 的所有文章" :en "View all posts tagged %s"}
   :back             {:zh "返回" :en "Back"}
   :posts-tagged     {:zh "标签 %s 的文章" :en "Posts tagged %s"}
   :loading          {:zh "加载中..." :en "Loading..."}
   ;; Theme toggle
   :switch-to-light  {:zh "切换到浅色模式" :en "Switch to light mode"}
   :switch-to-dark   {:zh "切换到深色模式" :en "Switch to dark mode"}
   :switch-to-auto   {:zh "切换到自动模式" :en "Switch to auto mode"}
   :theme-auto       {:zh "自动" :en "Auto"}
   :theme-light      {:zh "浅色" :en "Light"}
   :theme-dark       {:zh "深色" :en "Dark"}
   :theme-current    {:zh "当前主题：%s（点击切换）" :en "Theme: %s (click to change)"}})

(defn t
  "Translate a key to the current language. Supports format args.
   lang should be :zh or :en (defaults to :zh)"
  ([lang key] (t lang key nil))
  ([lang key args]
   (let [lang-key (or lang :zh)
         template (get-in translations [key lang-key] (name key))]
     (if args
       (apply gstr/format template (if (sequential? args) args [args]))
       template))))
