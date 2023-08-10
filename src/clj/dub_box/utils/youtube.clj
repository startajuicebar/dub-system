(ns dub-box.utils.youtube
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string]))

(defn every-third-line [text]
  (->> (clojure.string/split-lines text)
       (take-nth 3)))

(defn get-filename [file-url]
  (.getName (io/file file-url)))

(defn get-base-path [full-path]
  (.getParent (io/file full-path)))

(defn ensure-trailing-slash [path]
  (if-not (= \/ (last path))
    (str path "/")
    path))

(defn get-filename-without-extension [file-url]
  (let [filename (.getName (io/file file-url))
        name-parts (clojure.string/split filename #"\.")]
    (clojure.string/join "." (butlast name-parts))))

(defn extract-text-from-srt [srt]
  (->> (slurp srt)
       clojure.string/split-lines
       (partition 4)
       (map (fn [tuple]
              (nth tuple 2)))
       (filter (fn [word]
                 (not= \[ (first word))))))

(defn video->audio
  [video-url output-url]
  (sh "ffmpeg" "-i" video-url "-vn" "-acodec" "flac" output-url)
  output-url)

(defn audio->text
  [audio-url output-dir & {:keys [model]
                           :or {model "tiny.en"}}]
  (sh "whisper" "--model" model "--output_dir" output-dir "--output_format" "json" "--language" "en" "--task" "transcribe" audio-url)

  (str (ensure-trailing-slash output-dir) (get-filename-without-extension audio-url) ".json"))

(defn video->text
  [video-url & {:keys [audio-output text-output-dir]
                :or {audio-output (str
                                   (get-base-path video-url)
                                   "/"
                                   (get-filename-without-extension video-url)
                                   ".flac")
                     text-output-dir (get-base-path video-url)}}]
  (-> (video->audio video-url audio-output)
      (audio->text text-output-dir)))