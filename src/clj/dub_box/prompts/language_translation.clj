(ns dub-box.prompts.language-translation
  (:require [cheshire.core :refer [generate-string parse-string]]
            [dub-box.utils.language :as language-utils]))

(defn translate-utterances-context
  [from to]
  (let [from-lang (language-utils/convert-to-name from)
        to-lang (language-utils/convert-to-name to)]
    [{:role "system"
      :content (str "You are a professional language translator. You are excellent at translating language and passages into the most native tone and feeling. Today you are translating from " from-lang " to " to-lang ". The message being translated is for use in video dubbing. You only respond with json data structures just in the same structure as the input.")}
     {:role "user"
      :content (generate-string ["Hi how are you?",
                                 "What have you been up to?"])}
     {:role "assistant"
      :content (generate-string ["Salut! Comment allez-vous?",
                                 "Qu'avez-vous fait ?"])}]))

(comment

;;Keep from folding
  )