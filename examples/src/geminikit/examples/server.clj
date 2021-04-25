(ns geminikit.examples.server
  (:require [geminikit.server :as server]
            [clojure.java.io :as io])
  (:import [java.net InetSocketAddress]))

(def articles-dir "./examples/src/geminikit/examples/articles")
(def index-files ["/index.gemini" "/index.gmi"])

(defn as-relative [s] (if (= "/" (subs s 0 1)) (subs s 1) s))

(defn file [p f] (io/file p (as-relative f)))

(defn index-fallback [f]
  (if (.isDirectory f)
    (or (->> index-files
           (map #(file f %))
           (filter #(.isFile %))
           (first))
        f)
    f))

(defn not-found [msg] {:status 51 :meta msg :body ""})

(defn success [meta body] {:status 20 :meta meta :body body})

(defn gemtext [f] (success "text/gemini" (slurp (io/file f))))

(defn app [{path :path}]
  (let [f (index-fallback (file articles-dir path))] 
    (if (.isFile f)
      (gemtext f) 
      (not-found "🤷 Couldn't find it."))))

(defonce server (atom nil))

(defn up []
  (when-not @server
    (reset! server (server/start app {:socket-address (InetSocketAddress. "0.0.0.0" 1965)}))))

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
