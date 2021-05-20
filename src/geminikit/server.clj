(ns geminikit.server
  (:require [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [aleph.netty]
            [geminikit.codecs :refer [stream->request response->stream]]))

(defn- wrap-duplex-stream
  "handle stream by decoding source and encoding sink with gemini codecs"
  [s info]
  (let [out (s/stream)]
    (s/connect
      (response->stream out)
      s)
    (s/splice
      out
      (stream->request s info))))

(defn- request-handler
  "create a request handler from a function f.
   the request map is passed to f which should return a response map"
  [f]
  ;; The second parameter contains informations about the server and the client
  (fn [s]
    ;; take the request
    (-> (s/take! s)
        (d/chain
          ;; Process the request in another thread, it should return a response
          ;; TODO Shoudl we really do it in another thread? List pros/cons and reconsider
          ;; TODO Log request
          (fn [req] (d/future (f req)))
          ;; Once the transformation is complete, send the response to the client
          ;; TODO validate if result is valid
          ;; TODO Log response
          (fn [rsp] (s/put! s rsp)))
        ;; if there were any issues on the far end, send the appropriate error message
        (d/catch
          ;; Inform client that an error ocurred on the server
          ;; TODO implement logging mechanism and pass ex to it
          ;; TODO make exception handling pluggable
          (fn [_] (s/put! s {:status 42
                             :meta "Something went wrong on our side."
                             :body ""})))
        (d/finally
          ;; Gemini requires the connection to be closed always.
          (fn [] (s/close! s))))))

(defn middleware
  "Take fns as args and returna function that when called apply it's args
  to each function of fns sequentially and returns as soon as one returns
  a non nil value."
  [& fns]
  (fn [& args]
    (reduce
      (fn [_ f]
        (when-let [x (apply f args)]
          (reduced x)))
      nil
      fns)))

(defn start
  "start a gemini server with app-fn handling all requests
   the return is a java.lang.Closable and can be used to stop the server"
  ([app]
   (start app nil))
  ([app {:keys [port socket-address ssl-context]
         :or {port 1965
              ssl-context (aleph.netty/self-signed-ssl-context)}}]
   (tcp/start-server
     (fn [s info]
       (let [handler (request-handler app)
             s' (wrap-duplex-stream s info)]
         (handler s')))
     {:port port
      :socket-address socket-address
      :ssl-context ssl-context})))

