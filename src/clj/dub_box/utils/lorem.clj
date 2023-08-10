(ns dub-box.utils.lorem
  (:import [com.thedeanda.lorem LoremIpsum]))

(defn generate-words
  [word-count]
  (let [li (LoremIpsum/getInstance)]
    (.getWords li word-count)))

(defn generate-paragraphs
  [min max]
  (let [li (LoremIpsum/getInstance)]
    (.getParagraphs li min max)))