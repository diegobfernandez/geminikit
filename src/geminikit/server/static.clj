(ns geminikit.server.static
  (:require [clojure.java.io :as io]))

(def default-index-files
  ["index.gemini" "index.gmi"])
 
(defn- as-relative [s]
  (cond
    (empty? s) s
    (= "/" (subs s 0 1)) (subs s 1)
    :else s))
 
(defn- file
  ([f] (io/file f))
  ([f f'] (io/file f (as-relative f'))))
 
(defn- index-fallback
  "Given a java.io.File f and a list of filenames, when f is a directory
  returns the first existing file from the list of filenames"
  [f filenames]
  (if (.isDirectory f)
    (or (->> filenames
           (map #(file f %))
           (filter #(.isFile %))
           (first))
        f)
    f))

(defn serve-statics 
  [{:keys [rootdir index-files]
    :or {index-files default-index-files}}]
  (fn [{path :path}]
    (let [f (index-fallback (file rootdir path) index-files)] 
      (when (.isFile f)
        {:status 20
         :meta "text/gemini;charset=UTF-8"
         :body f}))))
 
