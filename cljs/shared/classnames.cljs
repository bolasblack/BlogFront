(ns shared.classnames
  (:require
   [clojure.string :as str]))

(defn ^:private clsx-single
  ;; "transform single clsx argument to string:
  ;;  (clsx-single {:a true :b false \"c\" true}) => [:a \"c\"]
  ;;  (clsx-single (when true :d)) => [:d]
  ;;  (clsx-single (when false :d)) => nil
  ;;  (clsx-single :e) => [:e]
  ;;  (clsx-single [:f :g]) => [:f :g]
  ;;  (clsx-single nil) => nil"
  [cs]
  (cond
    (nil? cs) nil
    (boolean? cs) nil
    (map? cs) (keep (fn [[k v]] (when v k)) cs)
    (coll? cs) (mapcat clsx-single cs)
    :else [cs]))

(defn clsx
  "like tailwind clsx, usage:
   (clsx
     {:a true :b false \"c\" true}
     (when true :d)
     :e
     [:f :g])
   => \"a c d e f g\"
  "
  [& cs]
  (->> (mapcat clsx-single cs)
       (map name)
       (str/join " ")))

(comment
  (let [x {:a true :b false "c" true}
        y1 (when false :d1)
        y2 (when true :d2)
        z :e
        w [:f :g]]
    (clsx-single x)
    (clsx x)
    (clsx x y1 y2 z w)))