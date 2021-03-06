(ns webchange.editor.events
  (:require
    [re-frame.core :as re-frame]
    [day8.re-frame.http-fx]
    [ajax.core :refer [json-request-format json-response-format]]
    [webchange.interpreter.events :as ie]))

(re-frame/reg-event-fx
  ::load-datasets
  (fn [{:keys [db]} _]
    (let [course-id (:current-course db)]
      {:db         (-> db
                       (assoc-in [:loading :datasets] true))
       :http-xhrio {:method          :get
                    :uri             (str "/api/courses/" course-id "/datasets")
                    :format          (json-request-format)
                    :response-format (json-response-format {:keywords? true})
                    :on-success      [::load-datasets-success]
                    :on-failure      [:api-request-error :datasets]}})))

(re-frame/reg-event-fx
  ::load-datasets-success
  (fn [{:keys [db]} [_ result]]
    {:db         (assoc-in db [:editor :course-datasets] (:datasets result))
     :dispatch-n (list [:complete-request :datasets])}))

(re-frame/reg-event-fx
  ::update-object
  (fn [{:keys [db]} [_ {:keys [scene-id target state]}]]
    {:db (assoc-in db [:scenes scene-id :objects (keyword target)] state)}))

(re-frame/reg-event-fx
  ::update-current-scene-object
  (fn [{:keys [db]} [_ {:keys [target state]}]]
    {:db (assoc-in db [:current-scene-data :objects (keyword target)] state)}))

(re-frame/reg-event-fx
  ::set-main-content
  (fn [{:keys [db]} [_ screen]]
    {:db (assoc-in db [:editor :current-main-content] screen)}))

(re-frame/reg-event-fx
  ::reset-object
  (fn [{:keys [db]} [_]]
    {:db (update-in db [:editor] dissoc :selected-object)}))

(re-frame/reg-event-fx
  ::reset-scene-action
  (fn [{:keys [db]} [_]]
    {:db (update-in db [:editor] dissoc :selected-scene-action)}))

(re-frame/reg-event-fx
  ::select-scene-action
  (fn [{:keys [db]} [_ action scene-id]]
    (when-not action (throw (js/Error. "Action is not defined")))
    (when-not scene-id (throw (js/Error. "Scene id is not defined")))
    (let [action-data (get-in db [:scenes scene-id :actions (keyword action)])]
      {:db       (assoc-in db [:editor :selected-scene-action] {:scene-id scene-id :action action})
       :dispatch [::set-form-data action-data]})))

(re-frame/reg-event-fx
  ::set-form-data
  (fn [{:keys [db]} [_ data]]
    {:db (assoc-in db [:editor :action-form] {:data data :path [] :breadcrumb []})}))

(re-frame/reg-event-fx
  ::show-form
  (fn [{:keys [db]} [_ form]]
    {:db (assoc-in db [:editor :shown-form] form)}))

(re-frame/reg-event-fx
  ::reset-shown-form
  (fn [{:keys [db]} [_]]
    {:db (update-in db [:editor] dissoc :shown-form)}))

(re-frame/reg-event-fx
  ::add-asset
  (fn [{:keys [db]} [_ {:keys [scene-id state]}]]
    (let [asset (assoc state :date (.getTime (js/Date.)))
          assets (-> db
                     (get-in [:scenes scene-id :assets] [])
                     (conj asset))]
      {:db (assoc-in db [:scenes scene-id :assets] assets)})))

(re-frame/reg-event-fx
  ::reset-scene-assets
  (fn [{:keys [db]} [_ scene-id assets]]
    {:db (assoc-in db [:scenes scene-id :assets] assets)}))

(re-frame/reg-event-fx
  ::reset-scene-actions
  (fn [{:keys [db]} [_ scene-id actions]]
    {:db (assoc-in db [:scenes scene-id :actions] actions)}))

(re-frame/reg-event-fx
  ::reset-scene-objects
  (fn [{:keys [db]} [_ scene-id objects]]
    {:db (assoc-in db [:scenes scene-id :objects] objects)}))

(re-frame/reg-event-fx
  ::reset-asset
  (fn [{:keys [db]} [_]]
    {:db (update-in db [:editor] dissoc :selected-asset)}))

(defn save-scene
  [db course-id scene-id scene-data]
  (let [data (dissoc scene-data :animations)]
    {:db         (-> db
                     (assoc-in [:loading :save-scene] true)
                     (update-in [:scenes scene-id] merge data))
     :http-xhrio {:method          :post
                  :uri             (str "/api/courses/" course-id "/scenes/" scene-id)
                  :params          {:scene data}
                  :format          (json-request-format)
                  :response-format (json-response-format {:keywords? true})
                  :on-success      [::save-scene-success]
                  :on-failure      [:api-request-error :save-scene]}}))

(re-frame/reg-event-fx
  ::save-current-scene
  (fn [{:keys [db]} [_ scene-id]]
    (let [course-id (:current-course db)
          scene-data (get-in db [:scenes scene-id])]
      (save-scene db course-id scene-id scene-data))))

(re-frame/reg-event-fx
  ::save-scene-success
  (fn [_ _]
    {:dispatch-n (list [:complete-request :save-scene])}))

(re-frame/reg-event-fx
  ::edit-dataset
  (fn [{:keys [db]} [_ dataset-id {:keys [fields]}]]
    {:db         (assoc-in db [:loading :edit-dataset] true)
     :http-xhrio {:method          :put
                  :uri             (str "/api/datasets/" dataset-id)
                  :params          {:scheme {:fields fields}}
                  :format          (json-request-format)
                  :response-format (json-response-format {:keywords? true})
                  :on-success      [::edit-dataset-success]
                  :on-failure      [:api-request-error :edit-dataset]}}))

(re-frame/reg-event-fx
  ::edit-dataset-success
  (fn [_ _]
    {:dispatch-n (list [:complete-request :edit-dataset]
                       [::load-datasets])}))

(re-frame/reg-event-fx
  ::load-current-dataset-items
  (fn [{:keys [db]} _]
    (let [dataset-id (get-in db [:editor :current-dataset-id])]
      {:db         (-> db
                       (assoc-in [:loading :dataset-items] true))
       :http-xhrio {:method          :get
                    :uri             (str "/api/datasets/" dataset-id "/items")
                    :format          (json-request-format)
                    :response-format (json-response-format {:keywords? true})
                    :on-success      [::load-current-dataset-items-success]
                    :on-failure      [:api-request-error :dataset-items]}})))

(re-frame/reg-event-fx
  ::load-current-dataset-items-success
  (fn [{:keys [db]} [_ result]]
    {:db         (assoc-in db [:editor :current-dataset-items] (:items result))
     :dispatch-n (list [:complete-request :dataset-items])}))

(re-frame/reg-event-fx
  ::show-add-dataset-item-form
  (fn [_ _]
    {:dispatch [::set-main-content :add-dataset-item-form]}))

(re-frame/reg-event-fx
  ::edit-dataset-item
  (fn [{:keys [db]} [_ item-id {:keys [data name]}]]
    {:db         (assoc-in db [:loading :edit-dataset-item] true)
     :http-xhrio {:method          :put
                  :uri             (str "/api/dataset-items/" item-id)
                  :params          {:data data :name name}
                  :format          (json-request-format)
                  :response-format (json-response-format {:keywords? true})
                  :on-success      [::edit-dataset-item-success]
                  :on-failure      [:api-request-error :edit-dataset-item]}}))

(re-frame/reg-event-fx
  ::edit-dataset-item-success
  (fn [_ _]
    {:dispatch-n (list [:complete-request :edit-dataset-item]
                       [::load-current-dataset-items]
                       [::set-main-content :dataset-info])}))

(re-frame/reg-event-fx
  ::update-dataset-item
  (fn [{:keys [db]} [_ id data-patch]]
    (let [{:keys [name data]} (get-in db [:dataset-items id])
          new-data (merge data data-patch)]
      {:db         (assoc-in db [:loading :update-dataset-item] true)
       :http-xhrio {:method          :put
                    :uri             (str "/api/dataset-items/" id)
                    :params          {:data new-data :name name}
                    :format          (json-request-format)
                    :response-format (json-response-format {:keywords? true})
                    :on-success      [::update-dataset-item-success]
                    :on-failure      [:api-request-error :update-dataset-item]}})))

(re-frame/reg-event-fx
  ::update-dataset-item-success
  (fn [{:keys [db]} [_ {:keys [id data]}]]
    {:db         (assoc-in db [:dataset-items id] data)
     :dispatch-n (list [:complete-request :update-dataset-item])}))

(re-frame/reg-event-fx
  ::select-current-scene
  (fn [{:keys [db]} [_ scene-id]]
    (if-not (nil? scene-id)
      {:dispatch-n (list
                     [::ie/set-current-scene scene-id]
                     [::reset-asset]
                     [::reset-object]
                     [::reset-scene-action]
                     [::reset-shown-form]
                     [::set-main-content :editor])}
      {})))
