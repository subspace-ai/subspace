(ns app.nodes
  (:require #?(:clj [app.xtdb-contrib :as db])
            #?(:clj [app.utils :as u])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [xtdb.api #?(:clj :as :cljs :as-alias) xt]))

(e/def !xtdb)
(e/def db) ; injected database ref; Electric defs are always dynamic

(e/defn Node [id]
  (e/server
    (let [e (xt/entity db id)
          status (:node/type e)]
      (e/client
        (dom/li (dom/props {:class "node"})
         (dom/div (dom/props {:class "node-text"
                              :contenteditable true})
                  (dom/text (:node/text e))
                  (dom/on "blur" (e/fn [e]
                                   (let [desc (.. e -target -innerHTML)]
                                     (e/server
                                      (e/discard
                                       (xt/submit-tx !xtdb [[:xtdb.api/put
                                                             {:xt/id id
                                                              :node/text desc
                                                              :node/type status}]]))))))))))))

(e/defn InputSubmit [F]
  (dom/input (dom/props {:placeholder "Insert node content.."})
    (dom/on "keydown" (e/fn [e]
                        (when (= "Enter" (.-key e))
                          (when-some [v (contrib.str/empty->nil (-> e .-target .-value))]
                            (new F v)
                            (set! (.-value dom/node) "")))))))

(e/defn NodeCreate []
  (e/client
    (InputSubmit. (e/fn [v]
                    (e/server
                      (e/discard
                        (xt/submit-tx !xtdb [[:xtdb.api/put
                                              {:xt/id (random-uuid)
                                               :node/text v
                                               :node/type :text
                                               :node/created-at (u/created-at)}]])))))))

#?(:clj
   (defn root-nodes [db]
     (->> (xt/q db '{:find [(pull ?e [*]) ?created-at]
                     :where [[?e :node/created-at ?created-at]]
                     :order-by [[?created-at :asc]]})
       (map first)
       vec)))

(comment (root-nodes user/db))

(e/defn Nodes []
  (e/server
    (binding [!xtdb user/!xtdb
              db (new (db/latest-db> user/!xtdb))]
      (e/client
        (dom/link (dom/props {:rel :stylesheet :href "/nodes.css"}))
        (dom/div (dom/props {:class "nodes"})
          (NodeCreate.)
          (dom/ul
            (e/server
              (e/for-by :xt/id [{:keys [xt/id]} (e/offload #(root-nodes db))]
                (Node. id)))))))))
