(ns browser.theme
  (:require [reagent.core :as r]
            [shared.i18n :as i18n]
            [browser.state :as st]))

;; ============================================================================
;; Theme State
;; ============================================================================

(def STORAGE_KEY "blog-theme")

;; Current theme preference (not the actual applied theme)
;; :system = follow OS preference
;; :light = always light
;; :dark = always dark
(defonce theme-preference (r/atom :system))

;; ============================================================================
;; Theme Detection & Application
;; ============================================================================

(defn get-system-theme
  "Detect system color scheme preference"
  []
  (if (and js/window.matchMedia
           (.-matches (js/window.matchMedia "(prefers-color-scheme: dark)")))
    :dark
    :light))

(defn apply-theme!
  "Apply theme to DOM by setting data-theme attribute on <html>"
  [preference]
  (let [html-el js/document.documentElement]
    (case preference
      :system (.removeAttribute html-el "data-theme")
      :light (.setAttribute html-el "data-theme" "light")
      :dark (.setAttribute html-el "data-theme" "dark"))))

(defn save-theme!
  "Persist theme preference to localStorage"
  [preference]
  (if (= preference :system)
    (.removeItem js/localStorage STORAGE_KEY)
    (.setItem js/localStorage STORAGE_KEY (name preference))))

(defn load-theme!
  "Load theme preference from localStorage"
  []
  (when-let [stored (.getItem js/localStorage STORAGE_KEY)]
    (keyword stored)))

;; ============================================================================
;; Theme Cycling
;; ============================================================================

(defn next-theme
  "Cycle through themes: system -> light -> dark -> system"
  [current]
  (case current
    :system :light
    :light :dark
    :dark :system))

(defn cycle-theme!
  "Cycle to next theme and apply it"
  []
  (let [new-theme (next-theme @theme-preference)]
    (reset! theme-preference new-theme)
    (apply-theme! new-theme)
    (save-theme! new-theme)))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-theme!
  "Initialize theme system on page load"
  []
  ;; Load saved preference or default to :system
  (let [saved (load-theme!)]
    (when saved
      (reset! theme-preference saved)))

  ;; Apply the current preference
  (apply-theme! @theme-preference)

  ;; Listen for system theme changes
  (when js/window.matchMedia
    (.addEventListener
     (js/window.matchMedia "(prefers-color-scheme: dark)")
     "change"
     (fn [_]
       ;; Only react if user preference is :system
       (when (= @theme-preference :system)
         ;; CSS handles this automatically, but we might want to trigger re-renders
         nil)))))

;; ============================================================================
;; UI Component
;; ============================================================================

(defn- t
  "Translate helper that uses current language from state"
  ([key] (i18n/t (:current-lang @st/state) key))
  ([key args] (i18n/t (:current-lang @st/state) key args)))

(defn theme-label
  "Get display label for current theme"
  [preference]
  (case preference
    :system (t :theme/auto)
    :light (t :theme/light)
    :dark (t :theme/dark)))

(defn- next-theme-label
  "Get label for the next theme (what clicking will switch to)"
  [current]
  (case current
    :system (t :theme/switch-to-light)
    :light (t :theme/switch-to-dark)
    :dark (t :theme/switch-to-auto)))

(defn ThemeToggle
  "Theme toggle button component"
  []
  (let [current @theme-preference]
    [:button.ThemeToggle
     {:on-click cycle-theme!
      :type "button"
      :aria-label (next-theme-label current)
      :title (t :theme/current (theme-label current))}
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

     [:span.ThemeToggle__label {:aria-hidden "true"} (theme-label current)]]))
