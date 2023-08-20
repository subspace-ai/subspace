(ns app.nodes
  (:import [hyperfiddle.electric Pending])
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

#?(:clj
   (defn upsert-li! [!xtdb & [?node ?text]]
     {:pre [!xtdb]}
     (let [id (:xt/id ?node (random-uuid))
           doc (if ?node
                 (merge ?node {:node/text ?text})
                 {:xt/id id
                  :node/text (or ?text "")
                  :node/type :text
                  :node/created-at (u/created-at)})]

       (xt/submit-tx !xtdb [[:xtdb.api/put doc]])
       id)))

(e/defn Node [id]
  (e/server
   (let [node (xt/entity db id)]
     (e/client
      (println "rendering" node)
      (dom/li (dom/props {:class "node"})
              (dom/div (dom/props {:class "node-text"
                                   :id (str "node-text-" id)
                                   :contenteditable true})
                       (set! (.-innerText dom/node) (-> node :node/text str))
                       (dom/on "keydown"
                               (e/fn [e]
                                 (println (.-key e))
                                 (cond (= "Enter" (.-key e))
                                       (let [v (-> e .-target .-innerText)]
                                         (.preventDefault e)
                                         (e/server
                                           (upsert-li! !xtdb node v) ; save current node
                                           (let [id (upsert-li! !xtdb nil "")] ; create new node
                                             (e/client (wait-for-element (str "node-text-" id)
                                                         (fn [e] (.focus e)))))))

                                       (and (= "Backspace" (.-key e))
                                            (= 0 (count (.. e -target -innerText))))
                                       (e/server
                                        (e/discard
                                         (xt/submit-tx !xtdb [[::xt/delete id]]))))))
                       (dom/on "blur"
                               (e/fn [e]
                                 (let [text (.. e -target -innerText)]
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

(comment
  (root-nodes (xt/db user/!xtdb))
  (upsert-li! user/!xtdb))

(e/defn Nodes []
  (e/server
   (binding [!xtdb user/!xtdb
             db (new (db/latest-db> user/!xtdb))]
     (e/client
      (dom/link (dom/props {:rel :stylesheet :href "/nodes.css"}))
       (try
         (dom/div (dom/props {:class "nodes"})
           (dom/ul
             (e/server
               (e/for-by :xt/id [{:keys [xt/id]} (root-nodes db) #_(e/offload #(root-nodes db))] ; Pending is causing over-rendering, fixme
                 (Node. id)))))
         (catch Pending _
           (dom/props {:style {:background-color "#e1e1e1"}})))))))
