(ns geminikit.server
  (:require [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [gloss.io :as glossio]
            [aleph.netty]
            [geminikit.codecs :refer [request-codec response-header-codec]]) 
  (:import [java.net InetSocketAddress]))

(defn- wrap-duplex-stream
  "handle stream by decoding source and encoding sink with gemini codecs"
  [s]
  (let [out (s/stream)]
    (s/connect
      (s/map #(glossio/encode response-header-codec %) out)
      s)
    (s/splice
      out
      (glossio/decode-stream s request-codec))))

(defn- request-handler
  "create a request handler from a function f.
   the request map is passed to f which should return a response map"
  [f]
  ;; The second parameter contains informations about the server and the client
  ;; Should it be passed along with the request to f?
  (fn [s _]
    ;; take the request
    (-> (s/take! s)
        (d/chain
          ;; Process the request in another thread, it should return a response
          (fn [req] (d/future (f req)))
          ;; Once the transformation is complete, send the response to the client
          (fn [rsp] (s/put! s rsp))
          ;; if we were successful in our response, close connection, as per gemini spec
          (fn [result]
            (when result
              (s/close! s))))
        ;; if there were any issues on the far end, send the appropriate error message
        ;; and close the connection
        (d/catch
          ;; TODO implement logging mechanism and pass ex to it
          ;; TODO make exception handling pluggable
          (fn [_]
            ;; TODO extract map to a helper function that validates the response
            (s/put! s {:status 42 :meta "Something went wrong on our side."})
            (s/close! s))))))

(defn start
  "start a gemini server with app-fn handling all requests
   the return is a java.lang.Closable and can be used to stop the server"
  ([app]
   (start app nil))
  ([app {:keys [socket-address ssl-context]
         :or {socket-address (InetSocketAddress. "localhost" 1965)
              ssl-context (aleph.netty/self-signed-ssl-context)}}]
   (tcp/start-server
     (fn [s info]
       (let [handler (request-handler app)
             s' (wrap-duplex-stream s)]
         (handler s' info)))
     {:socket-address socket-address
      :ssl-context ssl-context})))
