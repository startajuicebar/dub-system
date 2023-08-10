(ns dub-box.utils.debug)

(defn spy
  ([key v]
   (tap> {key v})
   v)
  ([v]
   (tap> {:spied v})
   v))