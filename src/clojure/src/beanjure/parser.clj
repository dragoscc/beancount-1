(ns beanjure.parser
  (:use [clojure.java.io :only (reader)]
        [clojure.pprint :only (pprint)]
        [clojure.string :only (split join)]
	)

  (:require [clojure.java.io :as jio])

  (:import [java.util Date])
  )


;;------------------------------------------------------------------------------

(def r-comment #"^[ \t]*;(.*)$")

(defn empty-line? [line]
  (re-matches #"^[ \t]*$" line))

(defn skip-line? [line]
  (or (empty-line? line)
      (re-matches r-comment line)))

;;------------------------------------------------------------------------------

(def r-date #"(\d\d\d\d)[/-](\d\d)[/-](\d\d)")

; A date within a note.
(def r-effdate (re-pattern (format "^%s(?:=%s)?" r-date r-date)))

;;------------------------------------------------------------------------------
(declare parse-defaccount parse-accid)

(def r-directive #"^@([a-z_]+)\s+([^;]*)(?:;.*)?")

(defn parse-directive [acc m]
  (let [[_ dirtype contents] m]
    (condp = dirtype
	"defaccount" (parse-defaccount acc m)
	"var" (parse-accid acc m)
	"begintag" [:begintag]
	"endtag" [:endtag]
	nil)))

;;------------------------------------------------------------------------------

(defrecord Account [kind name])

(def r-account #"[A-Z][A-Za-z0-9-]+(?::[A-Z][A-Za-z0-9-]*)*")

(def r-defaccount
  (re-pattern (format "(Cr|Dr)[ \t]+(%s)(?:[ \t]+(.*))?" r-account)))

(defn parse-defaccount [acc m]
  (if-let [mm (re-matches r-defaccount (nth m 2))]
    (new Account (nth mm 1) (keyword (nth mm 2)))
    (throw (Exception. (str "Invalid line:" (first m))))
    ))

;;------------------------------------------------------------------------------

(defrecord AccID [account id])

(def r-accid (re-pattern
	      (format "ofx[ \t]+accid[ \t]+([0-9A-Z-]+)[ \t]+(%s)" r-account)))

(defn parse-accid [acc m]
  (if-let [mm (re-matches r-accid (nth m 2))]
    (new AccID (keyword (nth mm 2)) (nth mm 1))
    (throw (Exception. (str "Invalid line:" (first m))))
    ))

;;------------------------------------------------------------------------------
(declare parse-top parse-posting)

(defrecord Transaction [date effdate flag check-no payee description postings])

(def r-transaction (re-pattern
		    (format "^%s[ \t]+(?:(.)[ \t]+)?(?:\\((.*?)\\))?(.*)$" r-effdate)))


(defn get-dates
  "Given 6 strings, the last 3 of which are optional, return a pair of dates."
  [m]
  (let [[s1 s2] (split-at 3 m)]
    [(apply #(Date. %1 %2 %3) (map #(Integer/parseInt %) (take 3 s1)))
     (when (first s2)
       (apply #(Date. %1 %2 %3) (map #(Integer/parseInt %) (take 3 s2))))]
    ))

(defn split-payee
  "Return (payee, description) if the separator is in the string; otherwise
  return the whole string as the description with (nil, description)."
  [s]
  (let [[a b :as c] (split s #"\|" 2)]
    (if (nil? b) [nil a] c)))

(def r-posting (re-pattern
		(format "^%s[ \t]+(?:(.)[ \t]+)?(?:\\((.*?)\\))?(.*)$" r-effdate)))


(defn parse-transaction [m lines acc]
  ;;(println "--- transaction" (first (drop 1 m)))
;;  (if-let [a (nth m 9)] (println a))
  (let [[d1 d2 :as d] (get-dates (subvec m 1 7)),
	flag (nth m 7),
	check-no (nth m 8),
	[payee description] (split-payee (nth m 9))
	]

    ;; Parse a list of postings.
    (loop [postings nil
	   line (first lines),
	   rest (next lines)]
      (if (or (nil? line) (empty-line? line))
	(fn [] (parse-top rest (update-in acc [:transactions] conj
                                          (Transaction. d1 d2 flag check-no payee description postings))))
	(recur (cons (parse-posting line) postings) (first rest) (next rest)))
      )))


;;------------------------------------------------------------------------------
(defrecord Posting [line])

(defn parse-posting [line]
  (Posting. line))

    ;; postaccount_re = re.compile('(?:%(accname)s|\[%(accname)s\]|\(%(accname)s\))' %
    ;;                             {'accname': account_re.pattern})

    ;; # Pattern for a posting line (part of a transaction).
    ;; posting_re = re.compile(
    ;;     ('\s+([*!]\s+)?(%(account)s)' # account name
    ;;      '(?:'
    ;;      '(?:\s+%(amount)s)?'  # main
    ;;      '(?:\s+(?:({)\s*%(amount)s\s*}|({{)\s*%(amount)s\s*}}))?' # declared cost
    ;;      '(?:\s+@(@?)(?:\s+%(amount)s))?\s*(?:;(.*))?\s*$'
    ;;      '|'
    ;;      '\s+(BOOK)\s+%(commodity)s(?:\s+(IN)\s+%(commodity)s)?\s*$'  # booking entry
    ;;      ')') %  # price/note
    ;;     {'amount': amount_re.pattern,
    ;;      'account': postaccount_re.pattern,
    ;;      'commodity': commodity_re.pattern})

;; # Pattern for a transaction line.
;; transaction_re = re.compile('^%(date)s(=%(date)s)?\s+(?:(.)\s+)?(\(.*?\))?(.*)$' %
;;                     {'date': date_re.pattern})
;; payee_sep = ' | '
;; desc_re = re.compile('(?:\s*([^|]+?)\s*\|)?\s*([^|]*?)\s*$')

    ;; # Pattern for an amount.
    ;; commodity_re = re.compile('"?([A-Za-z][A-Za-z0-9.~\']*)"?')
    ;; amount_re = re.compile('([-+]?\d*(?:\.\d*)?)\s+%(comm)s' %
    ;;                        {'comm': commodity_re.pattern})



;;------------------------------------------------------------------------------

(defn parse-top [lines acc]
  (let [line (first lines), rest (next lines)]
    (if (nil? line)
      acc
      ;; Skip empty lines.
      (if (skip-line? line)
	(fn [] (parse-top rest acc))

	;; Parse transactions.
	(if-let [m (re-matches r-transaction line)]
	  (fn [] (parse-transaction m rest acc))

	  ;; Parse directives.
	  (if-let [m (re-matches r-directive line)]
	    (let [directive (parse-directive acc m)]
	      (fn [] (parse-top rest (if directive
				       (update-in acc [:directives] conj directive)
				       acc))))

	    ;; Otherwise.
	    ;;(throw (Exception. (format "Invalid line: %s"  line)))
	    (fn [] (parse-top rest acc))
	    ))))))

;;------------------------------------------------------------------------------

(defn read-ledger [lines]
  (trampoline parse-top lines {})
  )

(comment

(def fname (str (System/getenv "HOME") "/q/accounting/blais.ledger"))

;;(dorun (map println (take 500 (line-seq (reader fname)))))


;; (defmethod print-method  [o, ^Writer w]
;;   (.write w (if (.fmID o)
;;     ))



(def ledger (time (read-ledger (line-seq (reader fname)))))

(keys ledger)
(-> ledger :transactions count)
(-> ledger :directives count)

(ledger :directives)

(-> ledger :transactions first :payee)



(do
  (println "----------------------------------------")
  (doseq [txn (:transactions ledger)]
    (if (re-find #"(?i)laika" (:description txn))
      (println txn))))

(dorun (map #(println (:description %)) (:transactions ledger)))

(float (/ (apply + (map #(-> % :postings count) (:transactions ledger))) (count (:transactions ledger))))


(pprint (:transactions (read-ledger (line-seq (reader fname)))))
(time (:transactions (read-ledger (read-lines fname))))
;(time (read-ledger (read-lines fname)))

;;  (pprint (read-ledger f)))

  )




;; Improvements:
;;   remove 'var ofx accid' -> @accid
;;   @check becomes -> @check-after @check-before
;;   only tags on postings, no notes vs. tags
;;   a new Exercise object; all Ledger objects serve Exercise
;;   Balsheet begin
;;   import should support bayesian
