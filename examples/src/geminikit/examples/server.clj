(ns geminikit.examples.server
  (:require [geminikit.server :as server]
            [geminikit.server.static :as static])
  (:import [java.net InetSocketAddress]))

(def articles-dir "./examples/src/geminikit/examples/articles")

(defn notfound-middleware [_]
  {:status 51
   :meta "🤷 Couldn't find it."
   :body ""})

(def app (server/middleware (static/serve-statics {:rootdir articles-dir})
                            notfound-middleware))

(defonce server (atom nil))

(defn up []
  (when-not @server
    (reset! server
            (server/start app {:socket-address
                               (InetSocketAddress. "0.0.0.0"
                                                   1965)}))))

(defn down []
  (when @server
    (reset! server (.close @server))))

(defn restart []
  (down)
  (up))

(comment
  (up)
  (down)
  (restart))
