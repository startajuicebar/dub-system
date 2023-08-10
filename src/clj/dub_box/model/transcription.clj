(ns dub-box.model.transcription
  (:require [clojure.string :refer [upper-case]]
            [dub-box.db.core :as db]
            [dub-box.utils.aws :as aws-utils]
            [dub-box.utils.debug :refer [spy]]
            [dub-box.utils.language :as language-utils]
            [dub-box.utils.deepgram :as deepgram-utils]
            [dub-box.model.utterance :as utterance-model]
            [dub-box.utils.open-ai :as open-ai-utils]
            [cheshire.core :refer [generate-string parse-string]]
            [dub-box.prompts.language-translation :refer [translate-utterances-context]]
            [honey.sql.helpers :as hh]
            [next.jdbc :as jdbc]
            [reitit.openapi :as openapi]))

(defn get-transcriptions
  []
  (db/get-rows! :transcription))

(defn get-by-id
  [id]
  (db/get-rows! :transcription
                :pred [:= :id id]
                :get-single? true))

(defn get-utterances-by-id
  [id]
  (db/get-rows! :utterance
                :pred [:= :transcription_id id]))

(defn create
  [video-content-id &
   {:keys [name native? language connection]
    :or {name "Untitled Transcription"
         native? true
         language "en"}}]
  (db/insert-row! :transcription {:id (java.util.UUID/randomUUID)
                                  :name name
                                  :video_content_id video-content-id
                                  :native native?
                                  :language [:cast (upper-case language) :language]}
                  {:connection connection}))

(defn get-transcription-audio-presigned-url
  [transcription-id]
  (let [audio-record (-> (hh/select-distinct :a.*)
                         (hh/from [:transcription_audio :ta])
                         (hh/join [:audio :a] [:= :a.id :ta.audio_id])
                         (hh/where [:= :ta.transcription_id transcription-id])
                         db/execute-one!)]

    (aws-utils/generate-presigned-url (:audio/bucket-key audio-record))))

(defn process-deepgram-result!
  [raw-result transcription-id]
  (jdbc/with-transaction [tx db/conn]
    (let [utterance-records (for [t raw-result]
                              (db/insert-row! :utterance {:id (java.util.UUID/randomUUID)
                                                          :transcription_id transcription-id
                                                          :start_time (:start t)
                                                          :end_time (:end t)
                                                          :channel (:channel t)
                                                          :transcript (:transcript t)}))]

      {:utterances utterance-records})))

(defn transcribe!
  [transcription-id]
  (-> transcription-id
      get-transcription-audio-presigned-url
      deepgram-utils/audio-url->text
      (process-deepgram-result! transcription-id)))

(defn generate-translated-utterances
  [utterances from to]
  (let [context (translate-utterances-context from to)
        utterance-transcripts (map :utterance/transcript utterances)
        utterance-transcripts-as-json (generate-string utterance-transcripts)
        payload (conj context {:role "user" :content utterance-transcripts-as-json})]
    (-> (open-ai-utils/make-chat-request payload)
        (parse-string true))))

(defn clone-to-target-language!
  [transcription-id to-language & {:keys [native?
                                          name
                                          from-language]
                                   :or {native? false
                                        name "Untitled"}}]

  (let [transcription-record (get-by-id transcription-id)
        utterance-records (get-utterances-by-id transcription-id)
        from-language (or from-language (:transcription/language transcription-record))
        translated-utterances (generate-translated-utterances utterance-records from-language to-language)
        zipped-results (zipmap utterance-records translated-utterances)
        cloned-transcription-record (db/insert-row! :transcription
                                                    {:id (java.util.UUID/randomUUID)
                                                     :name name
                                                     :source_transcription_id transcription-id
                                                     :video_content_id (:transcription/video-content-id transcription-record)
                                                     :native native?
                                                     :language [:cast (upper-case (language-utils/convert-to-iso-639-1 to-language)) :language]})

        cloned-utterance-records (for [[utterance-record translated-trascript] zipped-results]
                                   (db/insert-row! :utterance
                                                   {:id (java.util.UUID/randomUUID)
                                                    :transcription_id (:transcription/id cloned-transcription-record)
                                                    :start_time (:utterance/start-time utterance-record)
                                                    :end_time (:utterance/end-time utterance-record)
                                                    :channel (:utterance/channel utterance-record)
                                                    :source_utterance_id (:utterance/id utterance-record)
                                                    :transcript translated-trascript}))]
    {:transcription cloned-transcription-record
     :utterances cloned-utterance-records}))

(defn get-utterance-audios-by-id
  [transcription-id]
  (let [utterance-records (get-utterances-by-id transcription-id)]
    (-> (hh/select :ua.*)
        (hh/from [:utterance_audio :ua])
        (hh/join [:audio :a] [:= :a.id :ua.audio_id])
        (hh/where [:in :ua.utterance_id (map :utterance/id utterance-records)])
        db/execute!)))

(defn utterances-missing-audio
  [transcription-id]
  (let [utterance-records (get-utterances-by-id transcription-id)
        utterance-audio-records (get-utterance-audios-by-id transcription-id)]
    (filter (fn [u]
              (let [matches (some (fn [ua]
                                    (= (:utterance/id u)
                                       (:utterance-audio/utterance-id ua)))
                                  utterance-audio-records)]
                (nil? matches)))
            utterance-records)))

(comment

  (utterances-missing-audio "")
  (-> (hh/select :ua.*)
      (hh/from [:utterance_audio :ua])
      (hh/join [:audio :a] [:= :a.id :ua.audio_id])
      (hh/where [:in :ua.utterance_id ["3931bc85-e85d-4104-b0ea-6f2b5ff41dc5" "698687f3-0e54-4da6-9aaa-81702d2529fb"]])
      db/execute!)



  (count (utterances-missing-audio "6ca6ca20-0df8-43c3-a140-1da2972f8470"))
  (count (get-utterances-by-id "6ca6ca20-0df8-43c3-a140-1da2972f8470"))


  (generate-translated-utterances (get-utterances-by-id "d3a04227-7134-453a-a7e1-75a335bafff0") "en" "fr")
  ["Eh bien, en tant que quelqu'un qui n'a aucune expérience,"
   "de commencer un bar à jus, de tout savoir sur la gestion d'un bar à jus, j'étais un peu réticent à embaucher quelqu'un avec qui j'ai eu des désaccords."
   "mais je peux honnêtement dire que mon expérience avec en démarrage de bar à jus a été"
   "de premier ordre professionnelle,"
   "instructive,"
   "utile,"
   "du début à la fin. Et non seulement cela, mais j'ai passé un bon moment. J'ai vraiment apprécié de connaître le personnel et leur dévouement et serviable a rendu le processus amusant. Donc non seulement je ne suis pas"
   "effrayé de commencer mon bar à jus maintenant, mais j'ai hâte de le faire. Et je pense que c'est le meilleur cadeau de tous. Donc, je le recommande vivement à quiconque envisage cela."]

  (clone-to-target-language! "b8a9863b-3022-4339-9d21-2411f6e50be5" "FR")

  (for [x  [0 1]
        y [:a :b]]
    [x y])

  (for [[k v] (zipmap [{:id "hi"} {:id "ho"}] ["a" "b"])]
    [k v])

  (get-transcriptions)

  (create "377775f5-efcd-47a1-b139-0a25995210d0")

  (transcribe! "7a09a3bc-b478-40bc-924d-2605e2c991d4")

  (clone-to-target-language! "97cd0769-cb5b-4ad1-bca3-80fe772273f9" "fr")

  (prep-for-translation "d3a04227-7134-453a-a7e1-75a335bafff0")


  (-> (open-ai-utils/make-chat-request
       (conj (translate-utterances-context "English" "French")
             {:role "user"
              :content (generate-string (prep-for-translation "d3a04227-7134-453a-a7e1-75a335bafff0"))}))
      (parse-string true))
  ;; => ("Eh bien, en tant que quelqu'un qui n'a aucune expérience,"
  ;;     "en commençant un bar à jus, ne sachant rien sur la gestion d'un bar à jus, j'étais un peu réticent à embaucher quelqu'un avec qui j'étais en désaccord face à face."
  ;;     "mais je dirai honnêtement que mon expérience avec le bar à jus a été"
  ;;     "de premier ordre professionnel,"
  ;;     "informatif,"
  ;;     "utile,"
  ;;     "du début à la fin. Et pas seulement ça, j'ai passé un bon moment. J'ai vraiment aimé apprendre à connaitre le personnel et, leur disponibilité et leur amabilité ont rendu le processus amusant. Alors non seulement je suis,"
  ;;     "sans peur de démarrer mon bar à jus maintenant, mais j'ai hâte de le faire. Et je pense que c'est le meilleur cadeau de tous. Donc, je le recommande vivement à quiconque l'envisage.")


  (translate-utterances-context "English" "French")

  (conj (translate-utterances-context "English" "French")
        {:role "user"
         :content (prep-for-translation "d3a04227-7134-453a-a7e1-75a335bafff0")})

  (conj (translate-utterances-context "English" "French") {:role "user"
                                                           :content (prep-for-translation "d3a04227-7134-453a-a7e1-75a335bafff0")})

  (conj (translate-utterances-context "English" "French") {:role "user"
                                                           :content (prep-for-translation "d3a04227-7134-453a-a7e1-75a335bafff0")})

  (translate-utterances-context "English" "French")

;;Keep from folding
  )