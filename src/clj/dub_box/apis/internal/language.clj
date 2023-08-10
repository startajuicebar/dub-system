(ns dub-box.apis.internal.language
  (:require [dub-box.utils.deepgram :as deepgram-utils]
            [dub-box.utils.language :as language-utils]
            [potpuri.core]
            [reitit.coercion.spec]))

(def create-language-list
  (memoize
   (fn []
     (into [] (filter (fn [l]
                        (some #{(:code l)} language-utils/available-languages))
                      deepgram-utils/languages)))))

(defn list-available-languages-handler
  [_]
  {:status 200
   :body {:results (create-language-list)}})

(def routes
  ["/languages"
   {:swagger {:tags ["languages"]}}

   ["/get-available-languages"
    {:get {:summary "List all languages"
           :responses {200 {:body {:results vector?}}
                       400 {:body {:error string?}}}
           :handler list-available-languages-handler}}]])

(comment

  ;; Keep from folding
  )