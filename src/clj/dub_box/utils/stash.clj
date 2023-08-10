(ns dub-box.utils.stash
  (:require [dub-box.config :refer [env]]))

(defonce store (atom {}))

(defn stash
  ([val] (when (env :dev)
           (swap! store update-in [:default] conj val))
         val)
  ([key val] (when (env :dev)
               (swap! store update-in [key] conj val))
             (str "Stashed to " key)))

(defn peek-stash
  ([] (-> @store :default peek))
  ([key]  (-> @store key peek)))

(defn all-key
  [key]
  (-> @store key))

(defn all
  []
  @store)

(defn clear-key
  [key]
  (swap! store dissoc key)
  nil)

(defn clear-all
  []
  (reset! store {}))


(comment
  ;; Stash a value on a key
  (stash :son 20)

  ;; Peek the key
  (peek-stash :son)

  ;; Get all the vals for the key
  (all-key :son)

  ;; Clear the key
  (clear-key :son)

  ;; Return store
  (all)

  ;; Clear all keys in store
  (clear-all))
