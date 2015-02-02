(ns shout-client.core
  (:refer-clojure :exclude [name])
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [flux.dispatcher :as dispatcher :refer [dispatch!]]
            [om-tools.core :refer-macros [defcomponent]]
            [om-tools.dom :include-macros true]))

(def app-state (atom {:connect-page {}
                      :networks []}))

(defcomponent main
  [data owner]
  (render [_]
    (condp = (:active-screen data)
      nil       (om/build connect-page data)
      :connect  (om/build connect-page data)
      :chat     (om/build chat-window data)
      :settings (om/build settings-window data))))

(defn handle-ui-actions!
  ([store-atom] (handle-ui-actions! "" store-atom))
  ([prefix store-atom]
    (let [stream (partial dispatcher/stream (keyword prefix ui))]

      (stream :update-connect-form
        (fn [data]
          (let [e (:event data)
                field-name (.. e -target -name)
                field-val  (.. e -target -value)]
            (swap! store-atom assoc-in [:connect-form (keyword field-name)]
                                       field-val))))
      (stream :connect-to-network
        (fn []
          (.log js/console "trying to log in..")
          (.log js/console (clj->js @store-atom)))))))

(defcomponent connect-page
  [data owner]
  (render [_]
    (let [form-data (:connect-form data)]
      (html
        [:div {:id "connect-page"}
          [:h1 "Connect"]
          [:form {:id "connect-page-form"
                  :on-submit (fn [e] (.preventDefault e)
                                     (dispatch! :connect-to-network {}))
                  :on-change (fn [e] (.persist e)
                                     (dispatch! :update-connect-form {:event e :cursor data}))}
            [:h2 "Network settings"]
              [:input {:type "text" :name "network-name" :placeholder "Freenode" :default-value (:network-name form-data)}]
              [:input {:type "text" :name "network-uri" :placeholder "irc.freenode.org" :default-value (:network-uri form-data)}]
              [:input {:type "text" :name "network-port" :placeholder "6697" :default-value (:network-port form-data)}]
              [:input {:type "password" :name "network-password" :placeholder "" :default-value (:network-password form-data)}]
            [:h2 "User preferences"]
              [:input {:type "text" :name "user-nick" :placeholder "notbrent" :default-value (:user-nick form-data)}]
              [:input {:type "text" :name "user-username" :placeholder "notbrent" :default-value (:user-username form-data)}]
              [:input {:type "text" :name "user-real-name" :placeholder "Brent Vatne" :default-value (:user-real-name form-data)}]
              [:input {:type "text" :name "user-channels" :placeholder "#clojurescript, #clojure, #reactjs, #ionic" :default-value (:user-channels form-data)}]
            [:input {:type "submit" :value "Connect"}]]]))))

(defcomponent network-details
  [data owner]
  (render [_]
    (html [:div {:class "___"}
            (:name data)
            (doall (map (fn [channel]
                          [:div {:class "____"}
                            [:span {:class "channel-name"} (:name channel)]
                            [:span {:class "channel-unread-count"} (:unread channel)]])
                         (:channels data)))])))

(defcomponent sidebar-footer
  [data owner]
  (render [_]
    (html [:div {:id "sidebar-footer"}
           "sidebar footer goes here"])))

(defcomponent sidebar
  [data owner]
  (render [_]
    (html
      [:div {:id "sidebar"}
        (let [networks (:networks data)]
          (if (first networks)
            [:div {:class "__"}
              (om/build-all network-details networks)]
            [:div {:class "empty"}
              "You're not connected to any networks yet."]))
        (om/build sidebar-footer data)])))

(defcomponent app
  [data owner]
  (render [_]
    (html [:div {:id "app-container"}
           (om/build sidebar data)
           (om/build main data)])))
