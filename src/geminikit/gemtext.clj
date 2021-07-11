(ns geminikit.gemtext
  (:require [clojure.test :refer [is testing]]))

(def line-regex
  {:pre        #"^```$"
   :h3         #"^###\s*(?<text>[^#].+)$"
   :h2         #"^##\s*(?<text>.[^#].+)$"
   :h1         #"^#\s*(?<text>[^#].+)$"
   :link       #"^=>\s*(?<url>\S+)(?:\s(?<text>.*))?$"
   :list-item  #"^\* (?<text>.+)$"
   :quote      #"^\>\s*(?<text>.+)$"
   :text       #"^(?<text>.*)$"})

(def line-parse-order
  [:pre :h3 :h2 :h1 :link :list-item :quote :text])

(defn parse-line [line]
  (->> line-parse-order
       (filter #(re-matches (get line-regex %) line))
       first))

(defn parse-lines
  "Transform a seqeuence of lines to a sequence of marked lines"
  [lines]
  (->> lines
       (reduce
        (fn [acc line]
          (let [line-type (parse-line line)]
            (cond
              (= line-type :pre) (update acc :pre? not)
              (:pre? acc) (update acc :result conj [:pre line])
              :else (update acc :result conj [line-type line]))))
        {:result [] :pre? false})
       :result))

(def no-append (constantly false))

(defn with-tag [tag]
  (fn [_ [text] _] [tag text]))

(def gemdata-hiccup-map
  {:h1 [(with-tag :h1) no-append]
   :h2 [(with-tag :h2) no-append]
   :h3 [(with-tag :h3) no-append]
   :quote [(with-tag :quote) no-append]
   :text [(fn [_ [text] _] (when (seq text) [:p text]))
          no-append]
   :link [(fn [_ [url text] _] [:p [:a {:href url} text]])
          no-append]
   :pre [(fn [line _ append?]
           (if append?
             (str line "\r\n")
             [:pre [:code (str line "\r\n")]]))
         #(= % :pre)]
   :list-item [(fn [_ [text] append?]
                 (if append?
                   [:li text]
                   [:ul [:li text]]))
               #(= % :ul)]})

(defn gemdata->hiccup
  "Transform gemdata to hiccup"
  ([gemdata]
   (gemdata->hiccup gemdata :div))
  ([gemdata wrapper]
   (->> gemdata
        (reduce
         (fn [acc [type line]]
           (let [groups (rest (re-matches (get line-regex type) line))
                 [line-fn append-fn] (get gemdata-hiccup-map type)
                 append? (append-fn (first (last acc)))
                  ;; append? (if (> (count acc) 1)
                  ;;           (append-fn (first (last acc)))
                  ;;           false)
                 hiccup-line (line-fn line groups append?)]
             (cond
               (nil? hiccup-line) acc
               append? (update-in acc [(- (count acc) 1) 1] conj hiccup-line)
               (not append?) (conj acc hiccup-line)
               :else acc)))
         [])
        (into [wrapper]))))

(comment
  (testing "parse-line"
    (is (= :pre (parse-line "```")))
    (is (= :pre (parse-line "```")))
    (is (= :h3 (parse-line "###hello")))
    (is (= :h3 (parse-line "### hello")))
    (is (= :h3 (parse-line "###  hello")))
    (is (= :h2 (parse-line "##hello")))
    (is (= :h2 (parse-line "## hello")))
    (is (= :h2 (parse-line "##  hello")))
    (is (= :h1 (parse-line "#hello")))
    (is (= :h1 (parse-line "# hello")))
    (is (= :h1 (parse-line "#  hello")))
    (is (= :link (parse-line "=>/")))
    (is (= :link (parse-line "=>/ ")))
    (is (= :link (parse-line "=>/ hello")))
    (is (= :link (parse-line "=> /")))
    (is (= :link (parse-line "=> / ")))
    (is (= :link (parse-line "=> / hello")))
    (is (= :list-item (parse-line "* hello")))
    (is (= :quote (parse-line ">hello")))
    (is (= :quote (parse-line "> hello")))
    (is (= :text (parse-line "")))
    (is (= :text (parse-line "hello")))
    (is (= :text (parse-line "```hello")))
    (is (= :text (parse-line "#")))
    (is (= :text (parse-line "##")))
    (is (= :text (parse-line "###")))
    (is (= :text (parse-line ">")))
    (is (= :text (parse-line "*")))
    (is (= :text (parse-line "*hello"))))
  (testing "gemtext->data"
    (is (= [[:text "hello"]]
           (parse-lines ["hello"])))
    (is (= [[:pre "hello"]
            [:pre "* hello"]
            [:pre "=> /"]]
           (parse-lines
            ["```"
             "hello"
             "* hello"
             "=> /"
             "```"]))))
  (testing "gemdata->hiccup"
    (is (= [:div [:p "hello"]] (gemdata->hiccup [[:text "hello"]])))
    (is (= [:div [:pre [:code "hello\r\n"
                        "* hello\r\n"
                        "=> /\r\n"]]]
           (gemdata->hiccup
            [[:pre "hello"]
             [:pre "* hello"]
             [:pre "=> /"]])))))
