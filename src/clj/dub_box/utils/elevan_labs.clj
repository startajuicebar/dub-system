(ns dub-box.utils.elevan-labs
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [cheshire.core :as json]
   [dub-box.utils.audio :refer [play-audio]]
   [com.rpl.specter :as sp]
   [clojure.java.io :as io]
   [dub-box.config :refer [env]])

  (:import
   [javazoom.jl.player Player]
   [java.io FileInputStream]
   [java.net URL]
   [java.io ByteArrayOutputStream ByteArrayInputStream FileOutputStream])
  (:gen-class))

(def api-endpoint "https://api.elevenlabs.io/v1/text-to-speech/")

(defn play-stream [stream]
  (let [player (Player. stream)]
    (.play player)))

(defn play-file [file]
  (let [player (Player. (io/input-stream file))]
    (.play player)))

(defn generate-audio
  [text & {:keys [voice-id] :or {voice-id "ErXwobaYiN019PkySvjV"}}]
  (let [file-output (str "resources/audio/" (str (System/currentTimeMillis)) ".mp3")
        response (client/post (str api-endpoint voice-id)
                              {:headers {"xi-api-key" (-> env :eleven-labs :api-key)}
                               :content-type "application/json"
                               :accept :json
                               :as :stream
                               :body (json/generate-string {:text text
                                                            :model_id "eleven_multilingual_v1"
                                                            :voice_settings {:stability 0
                                                                             :similarity_boost 0}})})]
    (with-open [out (io/output-stream file-output)]
      (io/copy (:body response) out))
    file-output))

(comment

  (generate-audio "Persistence during challenging times is vital for achieving long-term goals. Hardships can be reframed as opportunities for growth, offering unparalleled insights into our capabilities and fostering resilience. Sticking with something tough is not just about overcoming obstacles, but also about personal evolution - it teaches us patience, discipline, and adaptability. Each difficulty conquered bolsters confidence, enhancing our problem-solving skills and decision-making ability. This commitment to stick it out, despite adversity, forms the bedrock of success. Remember, breakthroughs often occur when things seem most dire. Hence, embracing difficulty is essential, turning trials into triumphs and unlocking our full potential.")

  (-> (generate-audio "Hello there.")
      play-file)


  (-> (generate-audio "Y solo quiero grabar un video rápido porque algo ha estado rondando mi mente, y estaba pensando en cómo actualmente me encuentro en Ubud, Bali, en uno de mis cafeterías veganas favoritas, y reflexionaba sobre cómo, lamentablemente, muchas personas cuando inician negocios de jugos, lo hacen sin tener experiencia en negocios, se lanzan a ello movidos puramente por pasión.")
      play-file)

  (-> (generate-audio "E volevo solo registrare un breve video perché qualcosa mi stava frullando per la testa, e stavo pensando a come in realtà mi trovo ad Ubud, Bali in questo momento in uno dei miei caffè vegan preferiti, e stavo pensando a come purtroppo, quando molte persone iniziano i bar di succhi, lo fanno perché non hanno un background di business, lo stanno iniziando puramente per passione.")
      play-file)

  (-> (generate-audio "Et je voulais juste enregistrer une petite vidéo parce que quelque chose me préoccupe, et je pensais à comment je suis actuellement à Ubud, Bali, dans l'un de mes cafés végétaliens préférés, et je réfléchissais à comment malheureusement, beaucoup de personnes qui ouvrent des bars à jus le font sans avoir de formation en affaires, ils le font purement par passion." {:voice-id "oWAxZDx7w5VEj9dCyTzz"})
      play-file)

  (generate-audio "Y solo quiero grabar un video rápido porque algo ha estado rondando mi mente, y estaba pensando en cómo actualmente me encuentro en Ubud, Bali, en uno de mis cafeterías veganas favoritas, y reflexionaba sobre cómo, lamentablemente, muchas personas cuando inician negocios de jugos, lo hacen sin tener experiencia en negocios, se lanzan a ello movidos puramente por pasión.")



  (play-audio (generate-audio "Well, as someone who has zero experience.......... starting a juice bar---------- knowing anything about running a juice bar."))

  (clojure.string/join ["Well, as someone who has zero experience,"
                        "starting a juice bar, knowing anything about running a juice bar, I was a little reluctant to hire someone who I had messed with faced face."
                        "but I will honestly say that my experience with started juice bar has been"
                        "top notch professional,"
                        "informative,"
                        "helpful,"
                        "from start to finish. And not only that, I've had a great time. I really enjoyed getting to know the staff and, their willingness and helpfulness has has made it a fun process. So not only am I,"
                        "unafraid to start my juice bar now, but I'm actually looking forward to it. And I think that's the best gift of all. So I highly recommend him to anyone who's considering it."])

  "Well, as someone who has zero experience,starting a juice bar, knowing anything about running a juice bar, I was a little reluctant to hire someone who I had messed with faced face.but I will honestly say that my experience with started juice bar has beentop notch professional,informative,helpful,from start to finish. And not only that, I've had a great time. I really enjoyed getting to know the staff and, their willingness and helpfulness has has made it a fun process. So not only am I,unafraid to start my juice bar now, but I'm actually looking forward to it. And I think that's the best gift of all. So I highly recommend him to anyone who's considering it."







;;Keep from folding
  )