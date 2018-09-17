(ns browser.core
  (:require [reagent.core :as r]
            [browser.utils :refer [dom-ready]]))

(defn it-works []
  [:h1 "it works"])

(dom-ready #(r/render [it-works] (js/document.getElementById "app")))
