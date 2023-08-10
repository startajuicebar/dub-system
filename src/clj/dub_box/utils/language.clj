(ns dub-box.utils.language
  (:require [clojure.string :refer [lower-case]]))

(def available-languages ["en" "es" "fr" "hi" "it" "de" "pl" "pt"])

(def language-map {"en" "english"
                   "es" "spanish"
                   "fr" "french"
                   "hi" "hindi"
                   "it" "italian"
                   "de" "german"
                   "pl" "polish"
                   "pt" "portuguese"})

(def reverse-language-map (zipmap (vals language-map) (keys language-map)))

(defn get-by-iso
  [lang-str]

  (when lang-str
    (get language-map (lower-case lang-str))))

(defn get-by-name
  [lang-str]
  (when lang-str
    (get reverse-language-map (lower-case lang-str))))

(defn determine-encoding-type
  [lang-str]
  (let [iso-match (get-by-iso lang-str)
        name-match (get-by-name lang-str)]
    (if iso-match
      :iso
      (if name-match
        :name
        :unknown))))

(defn convert-to-iso-639-1
  [text]
  (let [encoding-type (determine-encoding-type text)]
    (case encoding-type
      :iso (lower-case text)
      :name (get-by-name text)
      nil)))

(defn convert-to-name
  [text]
  (let [encoding-type (determine-encoding-type text)]
    (case encoding-type
      :iso (get-by-iso text)
      :name (lower-case text)
      nil)))

(comment

  (convert-to-name "en")
  (determine-encoding-type "en")
  (get-by-iso "English")
  (get-by-iso "en")
  (get-by-name "English")

;;Keep from folding
  )