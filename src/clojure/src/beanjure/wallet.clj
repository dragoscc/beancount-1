(ns beanjure.wallet
  (:use [clojure.test]
	[clojure.reflect :only (reflect)]
	[clojure.algo.generic.functor :only (fmap)]
  	)

  ;; (:require [clojure.java.io :as jio])

  ;; (:import [java.util Date])
  )


(for [x (range 10)] (Thread/sleep 100))


(def wallet-empty? empty?)

(def wallet-add
  (partial merge-with +))

(def wallet-sub
  (partial merge-with -))

(def wallet-neg
  (partial fmap -))

(defn wallet-div
  [w divisor]
  (fmap #(/ % divisor) w))

(defn wallet-comm
  "Assuming that the wallet contains a single commodity, return the
   amount for that commodity. Fail if the Wallet is empty."
  [w]
  (if (> (count w) 1)
    (throw (Exception. (format "Cannot convert wallet %s to a single number." w)))
    (conj {} (first (seq w)))))

(defn wallet-nbthings
  "Return a single number, the total number of things that are stored in this
   wallet. (This is used for fiddling, as a really gross and inaccurate
   approximation of total amount.)"
  [w]
  (->> w vals (apply +)))

(defn wallet-only
  "Return a wallet like this one but with only the given commodity."
  [w comm]
  {comm (get w comm 0)})

(defn wallet-mask
  "Return this wallet with only the commodities present in the other wallet."
  [w wother]
  (into {} (map #(.entryAt w %) (keys wother))))





(deftest ops
  (is (wallet-empty? {}))
  (is (not (wallet-empty? {:USD 200})))

  (is (= (wallet-add {:USD 200} {:USD 1})
	 {:USD 201}))

  (is (= (wallet-sub {:USD 200} {:USD 1})
	 {:USD 199}))

  (is (= (wallet-neg {:USD 200 :CAD 201})
	 {:USD -200 :CAD -201}))

  (is (= (wallet-div {:USD 200 :CAD 100} 2)   {:USD 100 :CAD 50}))
  (is (= (wallet-div {:USD 200 :CAD 100} 0.1M) {:USD 2000M :CAD 1000M}))

  (is (= (wallet-comm {:USD 200})
	 {:USD 200}))

  (is (= (wallet-nbthings {:CAD 1 :USD 2 :JPY 3})
	 6))

  (is (= (wallet-only {:CAD 1 :USD 2 :JPY 3} :CAD)
	 {:CAD 1}))
  (is (= (wallet-only {} :CAD)
	 {:CAD 0}))

  (is (= (wallet-mask {:CAD 1 :USD 2 :JPY 3 :GBP 4} {:CAD 100 :JPY 300})
	 {:CAD 1 :JPY 3}))

  (is (thrown? Exception
	       (wallet-comm {:CAD 1 :USD 200})))
  )




(comment

(import [java.math BigDecimal])
(BigDecimal. 293)
(+ (BigDecimal. "293.032") (BigDecimal. "0.00000032"))

  (= (bigdec "0.1") 0.1M)
  
  (= (wallet-div {:USD 200M :CAD 100M} 0.1M)
     {:USD 2.00E+3M :CAD 1.00E+3M}
     {:USD 2000M :CAD 1000M})
  )




(with-precision 1
  (= 2.00E+3M 2000M))
(= 2E+3M 2000M)
(compare (bigdec "2000") (bigdec 2000)) 2.00E+3M

(= {:USD 2E+3M} {:USD 2000M})
(compare {:USD 2E+3M} {:USD 2000M})

(.longValue 2E+3M)
(.longValue 2000M)

(.scale 2E+3M)
(.scale 2000M)
(.equals 2E+3M 2000M)

(= 2E+3M 2000M)
(equals 2E+3M 2000M)
(equals 2E+3M 2000M)

(= 2E+1M 20M)

(.compareTo 2.00E+3M 2000M)
(.toString 2.00E+3M)
(.toString 2000M)


(=
 {:USD 2.00E+3M :CAD 1.00E+3M}
 {:USD 2000M :CAD 1000M})


;;------------------------------------------------------------------------------

(comment
  (defrecord Wallet [contents])
  (Wallet. {:USD 200})
  (let [w1 (Wallet. {:USD 200}),
        w2 (Wallet. {:USD 101})]
    (apply merge-with + (map :contents [w1 w2])))
  )


;;     def mask_commodity(self, com):
;;         "Return this wallet with only the given commodity."
;;         w = Wallet()
;;         num = self.get(com, None)
;;         if num is not None:
;;             w[com] = num
;;         return w

;;     def __str__(self):
;;         sitems = sorted(self.iteritems(), key=self.commodity_key)
;;         return ', '.join('%s %s' % (v, k) for k, v in sitems)

;;     def __repr__(self):
;;         return 'Wallet(%s)' % dict.__repr__(self)

;;     def tostrlist(self):
;;         """Return a list of pairs of strings (commodity, amount) to be
;;         rendered)."""
;;         return sorted(self.iteritems(), key=self.commodity_key)

;;     def tonum(self):
;;         """Assuming that the wallet contains a single commodity, return the
;;         amount for that commodity. If the Wallet is empty, return 0."""
;;         if len(self) == 0:
;;             d = Decimal()
;;         elif len(self) == 1:
;;             d = self.itervalues().next()
;;         else:
;;             raise ValueError("Cannot convert wallet %s to a single number." % self)
;;         return d

;;     def single(self):
;;         """Return a tuple of (amount, commodity) if this wallet contains a
;;         single thing. If empty or if it contains multiple things, blow up."""
;;         assert len(self) == 1, "Wallet contains more than one thing."
;;         c, a = self.iteritems().next()
;;         return (a, c)

;;     def __setitem__(self, key, value):
;;         if not isinstance(value, Decimal):
;;             value = Decimal(value)
;;         dict.__setitem__(self, key, value)

;;     def __nonzero__(self):
;;         return any(self.itervalues())

;;     def __neg__(self):
;;         return Wallet((k, -v) for k, v in self.iteritems())

;;     def round(self, mprecision=None):
;;         """
;;         Given a map of commodity to Decimal objects with a specific precision,
;;         return a rounded version of this wallet. (The default precision is
;;         provided by a key of None in the mprecision dict.)
;;         """
;;         if mprecision is None:
;;             mprecision = self.roundmap
;;         assert isinstance(mprecision, dict)
;;         w = Wallet()
;;         for com, amt in self.iteritems():
;;             try:
;;                 prec = mprecision[com]
;;             except KeyError:
;;                 prec = mprecision[None]
;;             w[com] = amt.quantize(prec)
;;         _clean(w)
;;         return w

;;     @staticmethod
;;     def commodity_key(kv):
;;         """ A sort key for the commodities."""
;;         k = kv[0]
;;         return (comm_importance.get(k, len(k)), k)

;;     def price(self, comm, ucomm, price):
;;         """ Replace all the units of 'comm' by units of 'ucomm' at the given
;;         price. """
;;         try:
;;             units = self[comm]
;;         except KeyError:
;;             return
;;         wdiff = Wallet()
;;         wdiff[comm] = -units
;;         wdiff[ucomm] = units * price
;;         self += wdiff

;;     def split(self):
;;         """ Split this wallet into two, one with all the positive unit values
;;         and one with all the negative unit values. This function returns two
;;         wallets which, summed together, should equal this wallet."""
;;         wpos, wneg = Wallet(), Wallet()
;;         zero = Decimal('0')
;;         for k, value in self.iteritems():
;;             w = wpos if value > zero else wneg
;;             w[k] = value
;;         return wpos, wneg

;;     def convert(self, conversions):
;;         """Given a list of (from-asset, to-asset, rate), convert the from-assets
;;         to to-assets using the specified rate and return a new Wallet with the
;;         new amounts."""
;;         w = self.copy()
;;         if conversions is None:
;;             return w
;;         assert isinstance(conversions, list)
;;         for from_asset, to_asset, rate in conversions:
;;             if from_asset in w:
;;                 if to_asset not in w:
;;                     w[to_asset] = Decimal()
;;                 w[to_asset] += w[from_asset] * rate
;;                 del w[from_asset]
;;         return w




