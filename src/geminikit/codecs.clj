(ns gemini-titan.codecs
  (:require [gloss.core :as g]))

;; Gemini requests are a single CRLF-terminated line with the following structure: 
;; <URL><CR><LF>
;; <URL> is a UTF-8 encoded absolute URL, including a scheme, of maximum length 1024 bytes.
(def request-codec
  (g/compile-frame
    (g/ordered-map
      :url (g/string :utf-8 :delimiters ["\r\n"]))))

;; Gemini response consist of a single CRLF-terminated header line, optionally followed by a response body. 
;; Gemini response headers look like this:
;; <STATUS><SPACE><META><CR><LF>
;; <STATUS> is a two-digit numeric status code, as described below in 3.2 and in Appendix 1.
;; <SPACE> is a single space character, i.e. the byte 0x20.
;; <META> is a UTF-8 encoded string of maximum length 1024 bytes, whose meaning is <STATUS> dependent.
;; <STATUS> and <META> are separated by a single space character.
(def response-header-codec
  (g/compile-frame
    (g/ordered-map
      :status (g/string-integer :utf-8 :suffix " ")
      :meta (g/string :utf-8 :suffix "\r\n")
      :body (g/string :utf-8))))
