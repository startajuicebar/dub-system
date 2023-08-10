(ns dub-box.utils.ffmpeg
  (:require [ffclj.core :as ffclj :refer [ffmpeg!]]
            [clojure.string :refer [join]]
            [dub-box.utils.debug :refer [spy]]
            [clojure.java.shell :refer [sh]]))

(defn concat-audio
  [audio-urls output-file]
  (let [i-args (reduce (fn [acc url]
                         (conj acc :i url))
                       []
                       audio-urls)
        filter-parts (join (for [i (range (count audio-urls))]
                             (str "[" i ":a]")))
        filter-arg (str filter-parts "concat=n=" (count audio-urls) ":v=0:a=1[aout]")
        final-args (conj i-args :filter_complex filter-arg :map "[aout]" output-file)]
    (with-open [task (ffmpeg! (spy :final-args final-args))]
      (.wait-for task)
      (println "Transcoding completed. Exit code: " (.exit-code task))
      output-file)))
