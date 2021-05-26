(ns geminikit.server.static
  (:require [clojure.java.io :as io]))

(def default-index-files
  ["index.gemini" "index.gmi"])
 
(defn as-relative
  "Remove trailing slash from s"
  [s]
  (cond
    (empty? s) s
    (= "/" (subs s 0 1)) (subs s 1)
    :else s))
 
(defn file
  "Coarces path into file.
  If two paths are provided they are concatenated."
  ([f] (io/file f))
  ([f f'] (io/file f (as-relative f'))))
 
(defn index-fallback
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
  "Create a middleware that returns files on the rootdir.
  Fallback to index-files if the path is a directory."
  [{:keys [rootdir index-files]
    :or {index-files default-index-files}}]
  (fn [{path :path}]
    (let [f (index-fallback (file rootdir path) index-files)] 
      (when (.isFile f)
        {:status 20
         ;; TODO: return the MIME type based on the content/extension
         :meta "text/gemini;charset=UTF-8"
         :body f}))))
 
