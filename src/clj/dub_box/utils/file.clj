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

(comment


  (require 'dub-box.utils.av)
  (with-temp-file
    (io/input-stream
     "/Users/atd/Documents/projects/sajb/dubbing-system/media/youtube-channel/Your Juice Or Smoothie Bar Business LOGO-8hZVKdQgHVY.mp4")
    (fn [file]
      (let [output-dir (use-or-create-dir "/Users/atd/Documents/projects/sajb/dubbing-system/tmp/")]
        (dub-box.utils.av/video->audio (.toString file) output-dir {:format "flac"}))))


  (dub-box.utils.av/video->audio
   "/var/folders/s0/pbtz1r8x3g34nyd8d3_dvqr80000gn/T/temp4087878969054203660.mp4"
   (str "/Users/atd/Documents/projects/sajb/dubbing-system/tmp/"
        (java.util.UUID/randomUUID)
        ".flac"))
  ;; => "/Users/atd/Documents/projects/sajb/dubbing-system/tmp/8e36cf85-4060-418e-a66f-afb6582c19fa.flac"

  ;; => "/Users/atd/Documents/projects/sajb/dubbing-system/tmp/e261c618-cae5-4828-a91e-510b2d30a24f.mp3"


  (-> (io/file "/Users/atd/Documents/projects/sajb/dubbing-system/media/samples/sample.mp3")
      .toString)

  ;; => #object[java.io.BufferedInputStream 0x465c8b85 "java.io.BufferedInputStream@465c8b85"]


 ;; => Execution error (IllegalArgumentException) at dub-box.utils.file/with-temp-file$fn (form-init9444107236339226714.clj:37).
 ;;    No matching method write found taking 1 args for class java.io.BufferedWriter

 ;; => nil


;;Keep from folding
  )