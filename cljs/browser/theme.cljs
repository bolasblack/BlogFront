(ns browser.theme
  (:require [reagent.core :as r]
            [shared.i18n :as i18n]
            [shared.components :as ui]
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
  "Theme toggle button component - uses shared component for hydration compatibility"
  [props]
  (let [current @theme-preference
        lang (:current-lang @st/state)]
    [ui/ThemeToggle {:class (:class props)
                     :lang lang
                     :on-click cycle-theme!
                     :theme current
                     :next-label (next-theme-label current)
                     :current-label (theme-label current)}]))
