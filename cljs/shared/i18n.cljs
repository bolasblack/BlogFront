(ns shared.i18n
  "Shared internationalization utilities"
  (:require [goog.string :as gstr]
            [goog.string.format]))

(def ^:private translations
  {;; UI - common interface elements
   :ui/post-list      {:zh "文章列表" :en "Post list"}
   :ui/post-info      {:zh "文章信息" :en "Post info"}
   :ui/tags           {:zh "标签" :en "Tags"}
   :ui/view-tag-posts {:zh "查看标签 %s 的所有文章" :en "View all posts tagged %s"}
   :ui/back           {:zh "返回" :en "Back"}
   :ui/posts-tagged   {:zh "标签 %s 的文章" :en "Posts tagged %s"}
   :ui/loading        {:zh "加载中..." :en "Loading..."}

   ;; Theme - theme toggle related
   :theme/switch-to-light {:zh "切换到浅色模式" :en "Switch to light mode"}
   :theme/switch-to-dark  {:zh "切换到深色模式" :en "Switch to dark mode"}
   :theme/switch-to-auto  {:zh "切换到自动模式" :en "Switch to auto mode"}
   :theme/auto            {:zh "自动" :en "Auto"}
   :theme/light           {:zh "浅色" :en "Light"}
   :theme/dark            {:zh "深色" :en "Dark"}
   :theme/current         {:zh "当前主题：%s（点击切换）" :en "Theme: %s (click to change)"}

   ;; SEO - meta tags and page titles
   :seo/site-title {:zh "c4605's blog" :en "c4605's blog"}
   :seo/site-desc  {:zh "关于编程和技术的个人博客" :en "A personal blog about programming and technology"}
   :seo/post-title {:zh "%s - c4605's blog" :en "%s - c4605's blog"}
   :seo/tag-title  {:zh "标签「%s」的文章 - c4605's blog" :en "Posts tagged \"%s\" - c4605's blog"}
   :seo/tag-desc   {:zh "所有标签为「%s」的文章" :en "All posts tagged with %s"}})

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
