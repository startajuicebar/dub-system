(ns dub-box.utils.date
  (:require [tick.core :as t]))

(defn instant-from-now
  "Generates an expiry date based on the current time, a duration, and a unit of measure.

  The function takes an expiry duration and an optional map with a key for 'uom' (unit of measure). 
  If no 'uom' is provided, it defaults to :days. 
  It returns a date-time object representing the expiry date.

  Example:
  (instant-from-now 1 {:uom :hours})

  Parameters:
  - expiry-duration: An integer representing the duration after which the date should expire.
  - uom: (optional) A keyword representing the unit of measure for the expiry duration. Defaults to :days."

  [expiry-duration & {:keys [:uom]
                      :or {uom :days}}]
  (t/>> (t/now)
        (t/new-duration expiry-duration uom)))

(comment
  (instant-from-now 1 {:uom :hours})
  (instant-from-now 1 {:uom :days})
  (instant-from-now 60 {:uom :seconds})

;;Keep from folding
  )