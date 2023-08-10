(ns dub-box.utils.aws
  (:import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           com.amazonaws.services.s3.model.CORSRule
           com.amazonaws.services.s3.model.ObjectListing
           org.joda.time.DateTime
           java.io.BufferedInputStream
           java.io.File
           java.io.InputStream
           java.io.FileInputStream
           java.text.SimpleDateFormat
           java.util.Date
           (java.nio.charset Charset)
           java.util.UUID
           java.security.KeyPairGenerator
           java.io.ByteArrayInputStream
           java.util.Base64
           java.security.SecureRandom)
  (:require
   [dub-box.config :refer [env]]
   [dub-box.utils.date :as date-utils]
   [dub-box.utils.file :as file-utils :refer [get-extention-by-type]]
   [com.rpl.specter :as sp]
   [amazonica.core :as am-core]
   [amazonica.aws.s3 :as s3]
   [tick.core :as t]
   [clojure.java.io :as io]
   [dub-box.utils.stash :as stash]
   [amazonica.core :as ac]))

(defn decode [to-decode]
  (.decode (Base64/getDecoder) to-decode))

(defn aws-creds
  []
  [(-> env :aws :access-key)
   (-> env :aws :secret-key)
   (-> env :aws :endpoint)])

(defn generate-presigned-url
  "Generates a presigned URL or URLs for specified key or keys in an S3 bucket.

  The function takes a key or a collection of keys and an optional map with keys for 'bucket-name', 'expiry-duration', and 'expiry-uom'. 
  If no 'bucket-name' is provided, it defaults to the 'default-bucket-name' from the AWS environment variables. 
  If no 'expiry-duration' is provided, it defaults to 6. 
  If no 'expiry-uom' is provided, it defaults to :days. 
  It returns a presigned URL or a collection of presigned URLs for the specified keys.

  Example:
  (generate-presigned-url \"my-key\" {:bucket-name \"my-bucket\" :expiry-duration 1 :expiry-uom :hours})

  Parameters:
  - key-or-keys: A string representing a key or a collection of keys for which to generate presigned URLs.
  - bucket-name: (optional) A string representing the name of the bucket. Defaults to the 'default-bucket-name' from the AWS environment variables.
  - expiry-duration: (optional) An integer representing the duration after which the presigned URL should expire. Defaults to 6.
  - expiry-uom: (optional) A keyword representing the unit of measure for the expiry duration. Defaults to :days."

  [key-or-keys & {:keys [bucket-name
                         expiry-duration
                         expiry-uom]
                  :or {bucket-name (-> env :aws :default-bucket-name)
                       expiry-duration 6
                       expiry-uom :days}}]
  (let [is-collection? (vector? key-or-keys)
        keys (if is-collection?
               key-or-keys
               [key-or-keys])

        result (for [key keys]
                 (-> (am-core/with-credential (aws-creds)
                       (s3/generate-presigned-url
                        bucket-name
                        key
                        (date-utils/instant-from-now expiry-duration expiry-uom)))
                     .toString))]

    (if is-collection?
      result
      (first result))))

(defn upload-to-s3
  "Uploads a payload to an S3 bucket.

  The function takes a map payload with keys for 'bucket-name' and 'key'. 
  It uploads the payload to the specified S3 bucket and returns a map with the upload response, key, and bucket name.

  Example:
  (upload-to-s3 {:bucket-name \"my-bucket\" :key \"my-key\" :file (io/file \"/path/to/myfile.txt\")})

  Parameters:
  - payload: A map containing the 'bucket-name', 'key', and the data to be uploaded. The data could be a file, stream, etc."

  [{:as payload
    :keys [bucket-name key]}]
  (let [upload (am-core/with-credential (aws-creds)
                 (s3/put-object payload))]
    {:upload upload
     :key key
     :bucket-name bucket-name}))

(defn upload-stream
  "Uploads a stream to an S3 bucket.

  The function takes a stream and a key, and an optional map with a key for 'bucket-name'. 
  If no 'bucket-name' is provided, it defaults to the 'default-bucket-name' from the AWS environment variables.

  Example:
  (upload-stream my-stream \"my-key\" {:bucket-name \"my-bucket\"})

  Parameters:
  - stream: The stream to be uploaded.
  - key: A string representing the key under which to store the stream in the bucket.
  - bucket-name: (optional) A string representing the name of the bucket to which the stream should be uploaded. Defaults to the 'default-bucket-name' from the AWS environment variables."

  [stream key & {:keys [bucket-name]
                 :or {bucket-name (-> env :aws :default-bucket-name)}}]

  (upload-to-s3 {:bucket-name  bucket-name
                 :key key
                 :input-stream stream}))

(defn s3-key-from-file
  "Generates a unique S3 key for a given file. 

  The function takes a file object and generates a unique key for it by appending a random UUID and the file's extension. 

  Example:
  (s3-key-from-file (io/file \"/path/to/myfile.txt\"))

  Parameters:
  - file: A File object for which the S3 key is to be generated."

  [file]
  (str
   (java.util.UUID/randomUUID)
   "."
   (file-utils/get-extention-by-file file)))


(defn upload-file
  "Uploads a file to an S3 bucket. 

  The function takes a file path or a file object and an optional map with keys for 'key' and 'bucket-name'. 
  If no 'bucket-name' is provided, it defaults to the 'default-bucket-name' from the AWS environment variables.

  Example:
  (upload-file \"/path/to/myfile.txt\" {:bucket-name \"my-bucket\" :key \"my-key\"})

  Parameters:
  - file-path-or-file: A string representing the file path or a File object to be uploaded.
  - key: (optional) A string representing the key under which to store the file in the bucket.
  - bucket-name: (optional) A string representing the name of the bucket to which the file should be uploaded. Defaults to the 'default-bucket-name' from the AWS environment variables."

  [file-path-or-file & {:keys [key bucket-name]
                        :or {bucket-name (-> env :aws :default-bucket-name)}}]

  (let [file (file-utils/as-file file-path-or-file)
        key (s3-key-from-file file)]
    (upload-to-s3 {:bucket-name  bucket-name
                   :key key
                   :file file})))

(defn list-s3-objects
  "Lists all objects in an S3 bucket.

  The function takes an optional map with a key for 'bucket-name'. 
  If no 'bucket-name' is provided, it defaults to the 'default-bucket-name' from the AWS environment variables.
  It returns a list of keys representing all objects in the specified S3 bucket.

  Example:
  (list-s3-objects {:bucket-name \"my-bucket\"})

  Parameters:
  - bucket-name: (optional) A string representing the name of the bucket from which to list objects. Defaults to the 'default-bucket-name' from the AWS environment variables."

  [& {:keys [bucket-name]
      :or {bucket-name (-> env :aws :default-bucket-name)}}]

  (->> (am-core/with-credential (aws-creds)
         (s3/list-objects-v2 {:bucket-name bucket-name}))
       (sp/select [:object-summaries sp/ALL :key])))