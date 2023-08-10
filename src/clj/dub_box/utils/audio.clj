(ns dub-box.utils.audio
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [cheshire.core :as json]
   [com.rpl.specter :as sp]
   [clojure.java.io :as io])

  (:import
   [javazoom.jl.player Player]
   [java.io FileInputStream]
   [java.net URL]
   [java.io ByteArrayOutputStream ByteArrayInputStream FileOutputStream])
  (:gen-class))

(defn play-audio [filename]
  (let [input-stream (FileInputStream. filename)
        player (Player. input-stream)]
    (.play player)
    filename))