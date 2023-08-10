(ns dub-box.utils.file
  (:require [clojure.java.io :as io]
            [clojure.string :refer [split]]
            [dub-box.utils.debug :refer [spy]]
            [dub-box.utils.av :as av-utils])
  (:import [java.io File InputStream FileOutputStream]
           [java.net URL]))

(def type-map {"image/jpg" "jpg"
               "image/jpeg" "jpg"
               "image/png" "png"
               "image/gif" "gif"
               "audio/mpeg" "mp3"
               "video/mp4" "mp4"
               "video/x-matroska" "mkv"
               "video/webm" "webm"})

(defn get-extention-by-type
  [type]
  (get type-map type))

(defn get-extention-by-file
  [file]
  (-> file
      .getName
      (split #"\.")
      last))

(defn as-file
  [path-or-file]
  (if (instance? File path-or-file)
    path-or-file
    (io/file path-or-file)))

(defn use-or-create-dir
  [dir]
  (let [file (io/file dir)]
    (when-not (.isDirectory file)
      (.mkdirs dir))
    (.getAbsolutePath (io/file dir))))

(defn with-temp-file [input-stream f & {:keys [name extention]
                                        :or {name "temp"
                                             extention "mp4"}}]
  (let [temp-file (File/createTempFile name extention)]
    (try
      ;; Write the input stream to the temp file
      (with-open [out (io/output-stream temp-file)]
        (io/copy input-stream out))

      ;; Operate on the temp file
      (f temp-file)

      ;; Ensure the temp file is deleted
      (finally
        (.delete temp-file)))))

(defn download-file [url-str save-path]
  (with-open [in-stream (.openStream (URL. url-str))
              out-stream (FileOutputStream. (File. save-path))]
    (let [buffer (byte-array 4096)
          read-bytes (fn [^InputStream s ^bytes b] (.read s b))]
      (loop [bytes-read (read-bytes in-stream buffer)]
        (when (pos? bytes-read)
          (.write out-stream buffer 0 bytes-read)
          (recur (read-bytes in-stream buffer))))))
  save-path)

(defn has-file?
  [filepath]
  (let [file (io/file filepath)]
    (.exists file)))