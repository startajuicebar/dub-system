(ns dub-box.utils.av
  (:require [dub-box.config :refer [env]]
            [cheshire.core :refer [generate-string parse-string]]
            [dub-box.utils.stash :refer [stash peek-stash]]
            [com.rpl.specter :as sp]
            [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [clojure.string]
            [clojure.java.io :as io]
            [dub-box.utils.debug :refer [spy]]
            [clj-http.client :as client]
            [clojure.string :as string]))

(defn lookup-format
  [format]
  (let [format-map {"mp3" "mp3"
                    "wav" "pcm_s16le"
                    "flac" "flac"
                    "ogg" "libvorbis"
                    "aac" "aac"
                    "wma" "wma"
                    "m4a" "m4a"
                    "mp4" "mp4"
                    "mkv" "mkv"
                    "webm" "webm"}]
    (get format-map format)))

(defn video->audio
  [video-url output-dir & {:keys [format]
                           :or {format "flac"}}]
  (let [output-file (str (java.util.UUID/randomUUID) "." format)
        output-path (str output-dir "/" output-file)
        converted-format (lookup-format format)
        has-format? converted-format]
    (if has-format?
      (do (sh "ffmpeg" "-i" video-url "-vn" "-acodec" (lookup-format format) output-path)
          (io/file output-path))
      nil)))