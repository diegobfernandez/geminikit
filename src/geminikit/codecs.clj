(ns geminikit.codecs
  (:require [clojure.java.io :as io]
            [manifold.stream :as s]
            [byte-streams :as b]))

(defn byte-seq [^java.io.InputStream is size]
  (let [ib (byte-array size)]
    ((fn step []
       (lazy-seq
         (let [n (.read is ib)]
           (when (not= -1 n)
             (let [bb (java.nio.ByteBuffer/wrap ib 0 n)]
               (cons bb (step))))))))))

(defn body-seq [x]
  (cond
    (instance? String x) (body-seq (.getBytes x))
    :else (io/input-stream x)))

(defn has-delimiter? [bs]
  (true? (reduce (fn [acc cur]
                   (if (= [(byte \return) (byte \newline)] [acc cur])
                     (reduced true)
                     cur))
               bs)))

(defn stream-read-line
  ;; TODO check if delimiter is at the very end of s
  [s]
  (s/buffer (fn [m] (if (has-delimiter? m) 1 0)) 1 s))

;; Gemini requests are a single CRLF-terminated line with the
;; following structure: 
;; <URL><CR><LF>
;; <URL> is a UTF-8 encoded absolute URL, including a scheme,
;; of maximum length 1024 bytes.
(defn stream->request [conn]
  (->> conn
       stream-read-line
       (s/transform
         (comp
           (map b/to-string)
           (map #(subs % 0 (- (count %) 2)))))))

;; Gemini response consist of a single CRLF-terminated header line,
;; optionally followed by a response body. 
;;
;; Gemini response headers look like this:
;; <STATUS><SPACE><META><CR><LF>
;; <STATUS> is a two-digit numeric status code,
;; as described below in 3.2 and in Appendix 1.
;; <SPACE> is a single space character, i.e. the byte 0x20.
;; <META> is a UTF-8 encoded string of maximum length 1024 bytes,
;; whose meaning is <STATUS> dependent.
;; <STATUS> and <META> are separated by a single space character.
(defn response->stream [conn]
  (let [dst (s/stream)
        is (atom nil)]
    (s/connect-via
      conn
      (fn [{:keys [status meta body] :or {body ""}}]
        (reset! is (body-seq body))
        (let [header (str status " " meta "\r\n")
              msgs (concat [header] (byte-seq @is 4096))]
          (s/put-all! dst msgs)))
      dst)
    (s/on-closed dst (fn []
                       (when @is
                         (.close @is))))
    dst))
