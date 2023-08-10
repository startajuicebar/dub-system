(ns dub-box.db.core
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.edn :as edn]
            [clojure.string]
            [clojure.tools.logging :as log]
            [dub-box.config :refer [env]]
            [hikari-cp.core :refer [close-datasource make-datasource]]
            [honey.sql :as sql]
            [honey.sql.helpers :as hh]
            [mount.core :refer [defstate]]
            [next.jdbc :as jdbc]
            [next.jdbc.date-time]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.result-set :as rs])
  (:import [java.sql PreparedStatement]
           [org.postgresql.util PGobject]))

(def ->json generate-string)
(def <-json #(parse-string % keyword))

(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (->json x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure
  data."
  [^org.postgresql.util.PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (<-json value) {:pgtype type}))
      value)))

;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))

  clojure.lang.IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^org.postgresql.util.PGobject v _2 _3]
    (<-pgobject v)))

(defstate conn
  :start (do
           (log/info "Connected to host" (-> env :pg :host))
           (make-datasource {:pool-name          "db-pool"
                             :adapter            "postgresql"
                             :username           (-> env :pg :user)
                             :password           (-> env :pg :password)
                             :database-name      (-> env :pg :name)
                             :server-name        (-> env :pg :host)
                             :port-number        (-> env :pg :port)
                             :register-mbeans    false}))
  :stop (close-datasource conn))

(defn format-sql [q]
  (sql/format q))

(defn execute-raw
  [sql & opts]
  (jdbc/execute! conn sql
                 (merge {:return-keys true
                         :builder-fn rs/as-kebab-maps
                         :pretty true} opts)))

(defn jdbc-exec!
  [func q & {:keys [connection opts debug]
             :or {opts {}
                  debug false}}]

  (let [sql (format-sql q)
        connection (or connection conn)]
    (if debug
      sql
      (func
       connection
       sql
       (merge {:return-keys true
               :builder-fn rs/as-kebab-maps} opts)))))

(defn execute!
  [q & extras]
  (apply jdbc-exec! jdbc/execute! q extras))

(defn execute-one!
  [q & extras]
  (apply jdbc-exec! jdbc/execute-one! q extras))

(defn insert-row!
  [table value & extras]
  (let [q (->
           (hh/insert-into table)
           (hh/values [value]))]
    (apply execute-one! q extras)))

(defn update-row!
  [table value pred]
  (->
   (hh/update table)
   (hh/set value)
   (hh/where pred)
   execute-one!))

(defn update-rows!
  [table value ids]
  (->
   (hh/update table)
   (hh/set value)
   (hh/where [:in :id ids])
   execute!))

(defn insert-rows!
  [table values]
  (->
   (hh/insert-into table)
   (hh/values values)
   execute!))

(defn destroy-rows!
  [table pred]
  (->
   (hh/delete-from table)
   (hh/where pred)
   execute!))

(defn execute-in-transaction!
  [statements]
  (jdbc/with-transaction [tx conn]
    (doseq [statement statements]
      (execute! statement :connection tx))))

(defn raw-columns->prepared-columns
  [columns]
  (cond
    (vector? columns) (mapv (fn [column] [(keyword column)]) columns)
    (string? columns) (keyword columns)
    :else :*))

(defn raw-params->prepared-order-by
  [params]
  (when
   (string? params) (->> (clojure.string/split params #":")
                         (mapv keyword))))

(defn total-pages
  [total-rows per-page]

  (-> (/ total-rows per-page)
      Math/ceil
      int
      dec
      (max 0)))

(def max-results (fnil min 100 100))

(defn get-rows!
  "Get rows from table. Can pass in options pred, columns, order
   Refer to honeysql docs for option formatting.

   For order clause
   '?order=updated_at' - ASC is default

   For DESC
   '?order=updated_at:desc'

   For predicate - Must be URL encoded. Refer to honey sql clauses
   '?pred=[:or [:= :id 1] [:= :id 2]]
   "
  [table & {:keys [columns
                   pred
                   order
                   get-single?
                   debug]}]
  (let [columns (raw-columns->prepared-columns columns)

        order (raw-params->prepared-order-by order)

        pred (if (string? pred)
               (edn/read-string pred)
               pred)

        query (-> (cond-> {:select columns}
                    true (hh/from table)
                    pred (hh/where pred)
                    order (hh/order-by order)))]
    (if debug
      (sql/format query)
      (if get-single?
        (execute-one! query)
        (execute! query)))))

(defn get-rows-paginated!
  "Get rows from table. Can pass in options pred, columns, page, per-page, order
   Refer to honeysql docs for option formatting.

   Max results are 100

   Page & per-page query params
   '?page=0&per-page=100'

   To use multiple columns filters add query params as follows:
   '?columns=id&columns=given_name'

   For order clause
   '?order=updated_at' - ASC is default

   For DESC
   '?order=updated_at:desc'

   For predicate - Must be URL encoded. Refer to honey sql clauses
   '?pred=[:or [:= :id 1] [:= :id 2]]
   "
  [table & {:keys [columns
                   page
                   per-page
                   pred
                   order
                   debug]}]

  (let [columns (raw-columns->prepared-columns columns)

        order (raw-params->prepared-order-by order)

        page (or page 0)

        pred (if (string? pred)
               (edn/read-string pred)
               pred)

        per-page (max-results per-page 100)

        total-rows (-> (cond-> (hh/select :%count.*)
                         true (hh/from table)
                         pred (hh/where pred))
                       execute-one!
                       :count)

        total-pages (total-pages total-rows per-page)

        offset (* page per-page)

        has-more? (< page total-pages)

        query (-> (cond-> {:select columns}
                    true (hh/from table)
                    true (hh/limit per-page)
                    true (hh/offset offset)

                    pred (hh/where pred)
                    order (hh/order-by order)))]

    (if debug
      {:results (sql/format query)
       :total-pages total-pages
       :has-more has-more?
       :page page
       :per-page per-page}
      {:results (execute! query)
       :total-pages total-pages
       :has-more has-more?
       :page page
       :per-page per-page})))

#_(get-rows-paginated! :ingredients)

(comment

  ;; Keep from folding
  )

