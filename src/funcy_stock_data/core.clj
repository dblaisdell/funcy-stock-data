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

(def yahoo-cache
  (atom (cache/ttl-cache-factory {} :ttl (* 60 60 12 1000))))

(defn yahoo-data
  [ticker]
  (if (cache/has? @yahoo-cache ticker)
    (get (cache/hit @yahoo-cache ticker) ticker)
    (get (swap! yahoo-cache
                #(cache/miss
                  %
                  ticker
                  (->> ticker
                       yahoo-url
                       io/reader
                       csv/read-csv)))
         ticker)))

(defn ticker-vecs
  [ticker]
  (->> ticker
       yahoo-data
       rest
       (mapv vec->tick)))

(defn ticker-maps
  [ticker]
  (->> ticker
       ticker-vecs
       (mapv )))


(comment
  (def tickers ["SPY" "TLT" "GLD" "IYR" "XLU"])
  (time (mapv ticker-vecs tickers))
  (time (mapv ticker-maps tickers))

  (time
   (-> "TLT"
       ticker-vecs))

  (time
   (-> "SPY"
       ticker-maps))

  )
