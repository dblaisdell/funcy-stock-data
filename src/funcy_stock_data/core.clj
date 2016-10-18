(ns funcy-stock-data.core
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.core.cache :as cache])
  (:import [java.time LocalDate]))


(set! *print-length* 10)

(defn yahoo-url
  [ticker]
  (str "http://ichart.finance.yahoo.com/table.csv?s=" ticker))

(defn vec->tick
  [[date open high low close volume adjclose]]
  [(LocalDate/parse date)
   (read-string open)
   (read-string high)
   (read-string low)
   (read-string close)
   (read-string volume)
   (read-string adjclose)])

(defn tick->map
  [[date open high low close volume adjclose]]
  {:date date
   :open open
   :high high
   :low low
   :close close
   :volume volume
   :adjclose adjclose})

(def cache (atom (cache/fifo-cache-factory {} :threshold 4)))

(defn cache-it [f & args]
  (let [k (keyword (apply str f args))]
    (if (cache/has? @cache k)
      (get (cache/hit @cache k) k)
      (get (swap! cache
                  #(cache/miss
                    %
                    k
                    (apply f args)))
           k))))

(defn yahoo-csv
  [ticker]
  (->> ticker
       yahoo-url
       io/reader
       csv/read-csv))

(defn- ticker-vecs
  [ticker]
  (->> ticker
       yahoo-data
       rest
       (mapv vec->tick)))

(def ticker-vecs
  (partial cache-it ticker-vecs))

(defn- ticker-maps
  [ticker]
  (->> ticker
       ticker-vecs
       (mapv tick->map)))

(def ticker-maps
  (partial cache-it ticker-maps))


(comment
  (def tickers ["SPY" "TLT" "GLD" "IYR" "XLU" "SLV"])
  (time (mapv ticker-vecs tickers))
  (time (mapv ticker-maps tickers))

  (time
   (-> "TLT"
       ticker-vecs))

  (time
   (-> "TLT"
       ticker-maps))

  )
