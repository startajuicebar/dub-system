(ns utils.language-test
  (:require
   [dub-box.utils.language :as tt]
   [clojure.test :refer :all]))

(deftest test-language-conversions
  (testing "correctly converts iso to name"
    (is (= (tt/get-by-iso "en") "english"))
    (is (= (tt/get-by-iso "fr") "french"))
    (is (= (tt/get-by-iso "invalid") nil)))

  (testing "correctly converts name to iso"
    (is (= (tt/get-by-name "english") "en"))
    (is (= (tt/get-by-name "ENGLISH") "en"))
    (is (= (tt/get-by-name "English") "en"))
    (is (= (tt/get-by-name "french") "fr"))
    (is (= (tt/get-by-name "invalid") nil)))

  (testing "correctly converts name to iso"
    (is (= (tt/get-by-name "english") "en"))
    (is (= (tt/get-by-name "ENGLISH") "en"))
    (is (= (tt/get-by-name "English") "en"))
    (is (= (tt/get-by-name "french") "fr"))
    (is (= (tt/get-by-name "invalid") nil)))

  (testing "correctly guesses the encoding type"
    (is (= (tt/determine-encoding-type "english") :name))
    (is (= (tt/determine-encoding-type "ENGLISH") :name))
    (is (= (tt/determine-encoding-type "FR") :iso))
    (is (= (tt/determine-encoding-type "en") :iso))
    (is (= (tt/determine-encoding-type "") :unknown)))

  (testing "correctly forces anything to iso-639-1"
    (is (= (tt/convert-to-iso-639-1 "english") "en"))
    (is (= (tt/convert-to-iso-639-1 "en") "en"))
    (is (= (tt/convert-to-iso-639-1 "EN") "en"))
    (is (= (tt/convert-to-iso-639-1 "invalid") nil)))

  (testing "correctly forces anything to name"
    (is (= (tt/convert-to-name "english") "english"))
    (is (= (tt/convert-to-name "en") "english"))
    (is (= (tt/convert-to-name "EN") "english"))
    (is (= (tt/convert-to-name "invalid") nil))))
