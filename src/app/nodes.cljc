(ns app.nodes
  (:require #?(:clj [app.xtdb-contrib :as db])
            #?(:clj [app.utils :as u])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [xtdb.api #?(:clj :as :cljs :as-alias) xt]))

(e/def !xtdb)
(e/def db) ; injected database ref; Electric defs are always dynamic

#?(:cljs
   (defn wait-for-element
     [element-id callback]
     (let [check-element (fn [interval-id-atom]
                           (when-let [element (js/document.getElementById element-id)]
                             (js/clearInterval @interval-id-atom)
                             (reset! interval-id-atom nil)
                             (callback element)))
           interval-id-atom (atom nil)]
       (reset! interval-id-atom (js/setInterval (partial check-element interval-id-atom) 50)))))

(e/defn Node [id]
  (e/server
   (let [node (xt/entity db id)
         text (-> node :node/text str)]
     (e/client
      (println "rendering" node)
      (dom/li (dom/props {:class "node"})
              (dom/div (dom/props {:class "node-text"
                                   :id (str "node-text-" id)
                                   :contenteditable true})
                       (dom/text text)
                       (dom/on "keydown"
                               (e/fn [e]
                                 (println (.-key e))
                                 (cond (= "Enter" (.-key e))
                                       (do (.preventDefault e)
                                           (e/server
                                            (let [created-at (u/created-at)
                                                  doc {:xt/id (random-uuid)
                                                       :node/text ""
                                                       :node/type :text
                                                       :node/created-at created-at}]
                                              (xt/submit-tx !xtdb [[:xtdb.api/put doc]])
                                              (e/client (wait-for-element (str "node-text-" (doc :xt/id))
                                                                          (fn [e] (.focus e)))))))

                                       (and (= "Backspace" (.-key e))
                                            (= 0 (count (.. e -target -innerHTML))))
                                       (e/server
                                        (e/discard
                                         (xt/submit-tx !xtdb [[::xt/delete id]]))))))
                       (dom/on "blur"
                               (e/fn [e]
                                 (let [text (.. e -target -innerHTML)]
                                   (e/server
                                    (e/discard
                                     (xt/submit-tx !xtdb [[:xtdb.api/put (merge node {:node/text text})]]))))))))))))

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
               (dom/ul
                (e/server
                 (e/for-by :xt/id [{:keys [xt/id]} (e/offload #(root-nodes db))]
                           (Node. id)))))))))
