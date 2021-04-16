(ns gemini-titan.example
  (:require [gemini-titan.server :as server])
  (:import [java.net URI InetSocketAddress]))

(defn as-uri [s]
  (bean (URI. s)))

;; TODO move router to separate namespace
(defn router
  [req]
  (update req :url as-uri))

(defn app [req]
  ;; if query string is present it means the user responded
  (if (not (-> req :url :query))
    ;; if no query string is present request the user name
    {:status 10 :meta "What's your name?" :body ""}
    ;; otherwise say hello
    {:status 20 :meta "text/gemini"
     :body (str "Hello " (-> req :url :query))}))

(defonce server (atom nil))

(defn up []
  (when-not @server
    (reset! server (server/start (comp app router)
                                 {:socket-address (InetSocketAddress. "0.0.0.0" 1965)}))))

(defn down []
  (when @server
    (reset! server (.close @server))))

(defn restart []
  (down)
  (up))

(comment
  (up)
  (down)
  (restart)
  (.port @server))
