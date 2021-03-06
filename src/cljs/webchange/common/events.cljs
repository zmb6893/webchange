(ns webchange.common.events
  (:require
    [clojure.string :as s]
    [re-frame.core :as re-frame]
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [webchange.interpreter.variables.core :refer [variables]]
    [webchange.interpreter.renderer.state.scene :as scene]
    [webchange.interpreter.renderer.state.overlays :as overlays]
    [webchange.interpreter.renderer.scene.components.wrapper-interface :as w]))

(def executors (atom {}))
(def flows (atom {}))
(def timers (atom {}))

(declare get-action)
(declare execute-action)

(defn reg-executor
  [id executor]
  (swap! executors assoc id executor))

(defn prepare-params
  [action params from-params]
  (reduce-kv (fn [m k v] (assoc m k (get params (keyword v)))) action from-params))

(defn prepare-action
  [{:keys [params from-params var from-var] :as action}]
  (-> action
      (prepare-params params from-params)
      (prepare-params var from-var)))

(defn reg-simple-executor
  [id event-name]
  (let [handler (fn [{:keys [action]}] (re-frame/dispatch [event-name (prepare-action action)]))]
    (reg-executor id handler)))

(reg-executor :action
              (fn [{:keys [db action]}]
                "Execute `action` action - call another action by its name.

                Action params:
                :id - callable action name.

                Example 1: simple action call
                {:type 'action',
                 :id   'init-audio-correct'}

                Example 2: call concepts action
                {:type     'action',
                 :from-var [{:var-name     'current-concept',
                             :var-property 'mari-word'}

                Example 3: call action with params
                {:type   'action',
                 :id     'set-current-tool',
                 :params {:tool 'felt-tip'}}

                Example 4: call action by name from variable
                {:type     'action'
                 :from-var [{:action-property 'id',
                             :var-name        'current-audio-correct',
                             :possible-values [:mari-audio-correct-1 :mari-audio-correct-2]}]}"
                (execute-action db (-> action
                                       :id
                                       (get-action db action)
                                       (assoc :flow-id (:flow-id action))
                                       (assoc :action-id (:action-id action))))))

(reg-simple-executor :sequence ::execute-sequence)
(reg-simple-executor :sequence-data ::execute-sequence-data)
(reg-simple-executor :parallel ::execute-parallel)
(reg-simple-executor :remove-flows ::execute-remove-flows)
(reg-simple-executor :remove-flow-tag ::execute-remove-flow-tag)
(reg-simple-executor :callback ::execute-callback)
(reg-simple-executor :hide-skip ::execute-hide-skip)


(def on-skip-handlers
  "A list of functions to invoke on skip actions.
  Is used to cancel current action (e.g. stop audio, finish transition) and continue the flow (success event)."
  (atom []))

(defn on-skip!
  "Register function to invoke on skip action"
  [handler]
  (when handler
    (swap! on-skip-handlers conj handler)))

(defn success-event
  [{:keys [flow-id action-id]}]
  [::flow-success flow-id action-id])

(declare flow-success!)

(defn dispatch-success-fn
  [{:keys [flow-id action-id]}]
  (flow-success! flow-id action-id))

(defn get-action-tags
  [{:keys [tags unique-tag skippable]}]
  (cond-> tags
          unique-tag (conj unique-tag)
          skippable (conj "skip")))

(defn flow-not-registered?
  [tag]
  (not-any? #(contains? (:tags %) tag) (vals @flows)))

(def event-as-action
  "Interceptor
  Transform event arguments to action-data"
  (re-frame/->interceptor
    :before (fn [context]
              (update-in context [:coeffects :event] #(second %)))))

(declare register-flow!)

(defn ->with-flow
  [action]
  (let [flow-id (:flow-id action)]
    (if flow-id
      action
      (let [flow-id (random-uuid)
            action-id (random-uuid)]
        (register-flow! {:flow-id       flow-id
                         :actions       [action-id]
                         :current-scene (:current-scene action)
                         :type          :all
                         :tags          (get-action-tags action)})
        (assoc action :flow-id flow-id :action-id action-id)))))

(def with-flow
  "Interceptor
  Add and register flow if it is not defined"
  (re-frame/->interceptor
    :before (fn [context]
              (let [action (-> context
                               (get-in [:coeffects :event])
                               (assoc :current-scene (get-in context [:coeffects :db :current-scene]))
                               ->with-flow)]
                (assoc-in context [:coeffects :event] action)))))

(defn with-prev
  [action prev]
  (-> action
      (assoc :var (:var prev))
      (update-in [:params] merge (:params prev))))

(defn from-template
  [template value]
  (if template
    (clojure.string/replace template "%" value)
    value))

(defn with-var-property
  []
  (fn [action {:keys [var-name var-property var-key action-property template to-vector]}]
    (let [var (get @variables var-name)
          value (cond->> var
                         var-property ((keyword var-property))
                         var-key (hash-map (keyword var-key))
                         to-vector (conj [])
                         template (from-template template))
          should-merge-to-root (and var-name (not action-property))]
      (if should-merge-to-root
        (merge (dissoc action :from-var) value)
        (assoc-in action (map keyword (s/split action-property #"\.")) value)))))

(defn with-var-properties
  [action]
  (if-let [from-var (:from-var action)]
    (reduce (with-var-property) action from-var)
    action))

(defn with-param-property
  [action {:keys [param-property action-property template]}]
  (let [value (get-in action [:params (keyword param-property)])]
    (assoc action (keyword action-property) (from-template template value))))

(defn with-param-properties
  [action]
  (if-let [from-params (:from-params action)]
    (reduce with-param-property action from-params)
    action))

(defn with-progress-property
  [db]
  (fn [action {:keys [progress-property action-property template]}]
    (let [value (get-in db [:progress-data :variables (keyword progress-property)])]
      (assoc action (keyword action-property) (from-template template value)))))

(defn with-progress-properties
  [action db]
  (if-let [from-progress (:from-progress action)]
    (reduce (with-progress-property db) action from-progress)
    action))

(defn with-var-object-property
  [db]
  (fn [action {:keys [var-name object-name-template object-property action-property offset]}]
    (let [var (get @variables var-name)
          object-name (from-template object-name-template var)
          object (->> (keyword object-name)
                      (scene/get-scene-object db))]

      (when (nil? object)
        (-> (str "Object with name <" object-name "> was not found") js/Error. throw))

      (let [object-data (w/get-data object)
            object-property-path (map keyword (clojure.string/split object-property "."))
            object-property-value (let [val (get-in object-data object-property-path)]
                                    (if offset (+ val offset) val))
            action-property-path (map keyword (clojure.string/split action-property "."))]

        (when (nil? object-property-value)
          (-> (str "Object <" object-name "> doesn't have property <" object-property-path ">") js/Error. throw))

        (assoc-in action action-property-path object-property-value)))))

(defn with-var-object-properties
  [action db]
  (if-let [from-var-object (:from-var-object action)]
    (reduce (with-var-object-property db) action from-var-object)
    action))

(defn ->with-vars
  [db action]
  (-> action
      (with-param-properties)
      (with-var-properties)
      (with-progress-properties db)
      (with-var-object-properties db)))

(def with-vars
  (re-frame/->interceptor
    :before (fn [context]
              (let [{:keys [db event]} (:coeffects context)
                    action (->with-vars db event)]
                (assoc-in context [:coeffects :event] action)))))

(defn get-action
  ([id db]
   (get-action id db {}))
  ([id db prev]
   (let [action (get-in db [:scenes (:current-scene db) :actions (keyword id)])]
     (if-not (nil? action)
       (-> action
           (with-prev prev))
       (-> (str "Action '" id "' was not found") js/Error. throw)))))

(declare discard-flow!)
(declare register-flow-tags!)

(defn execute-action
  [db {:keys [unique-tag] :as action}]
  (when (flow-not-registered? unique-tag)
    (let [{:keys [type return-immediately flow-id tags] :as action} (as-> action a
                                                                          (assoc a :current-scene (:current-scene db))
                                                                          (->with-flow a)
                                                                          (->with-vars db a))
          handler (get @executors (keyword type))]
      (when tags
        (register-flow-tags! flow-id tags))
      (handler {:db db :action action})
      (when return-immediately
        (dispatch-success-fn action)))))

(re-frame/reg-event-fx
  ::execute-action
  [event-as-action]
  (fn-traced [{:keys [db]} action]
    (execute-action db action)
    {}))

(defn remove-tag
  [flow tag]
  (let [tags (->> (:tags flow)
                  (remove #{tag})
                  (into #{}))]
    (assoc flow :tags tags)))

(defn add-tags
  [flow tags]
  (let [tags (->> (:tags flow)
                  (concat tags)
                  (into #{}))]
    (assoc flow :tags tags)))

(defn destroy-timer
  [timer]
  (case (:type timer)
    "interval" (.clearInterval js/window (:id timer))
    (throw (js/Error. (str "Timer type '" (:type timer) "' is not supported")))))

(re-frame/reg-event-fx
  ::execute-register-timer
  (fn [{:keys [db]} [_ {:keys [name] :as timer}]]
    (swap! timers assoc name timer)
    {}))

(re-frame/reg-event-fx
  ::execute-remove-timer
  (fn [{:keys [db]} [_ {:keys [name]}]]
    (let [timer (get @timers name)]
      (when-not (nil? timer)
        (destroy-timer timer))
      (swap! timers dissoc name)
      {})))

(defn remove-timers!
  []
  (let [timers-to-remove (->> @timers
                              (map second))]
    (doseq [timer timers-to-remove]
      (destroy-timer timer))
    (reset! timers {})))

(re-frame/reg-event-fx
  ::execute-remove-timers
  (fn [{:keys [db]} [_]]
    (remove-timers!)
    {}))

(defn flow-finished?
  [{:keys [type actions succeeded]}]
  (case type
    :all (if (= (into #{} actions) succeeded) true false)
    :any (if (not-empty succeeded) true false)
    false))

(defn check-flow!
  [flow-id]
  (let [{:keys [next] :as flow} (get @flows flow-id)]
    (when (flow-finished? flow)
      (swap! flows dissoc flow-id)
      (when next
        (next)))))

(defn register-flow!
  [{:keys [flow-id current-scene] :as flow}]
  (let [current-flow (get @flows flow-id)
        original-tags (into #{} (:tags current-flow))
        flow-data (-> current-flow
                      (merge flow)
                      (update-in [:tags] #(into original-tags %))
                      (update-in [:tags] conj (str "scene-" current-scene)))]
    (swap! flows assoc flow-id flow-data)))

(defn discard-flow!
  [flow-id]
  (let [flow (get @flows flow-id)
        handler (:on-remove flow)]
    (when handler
      (handler))
    (swap! flows dissoc flow-id)))

(defn register-flow-remove-handler!
  [flow-id handler]
  (swap! flows update-in [flow-id :on-remove] conj handler))

(defn register-flow-tags!
  [flow-id tags]
  (when flow-id
    (swap! flows update flow-id add-tags tags)))

(defn flow-ancestors
  [flow-id]
  (let [flow (get @flows flow-id)
        parent (:parent flow)]
    (if parent
      (concat [flow] (flow-ancestors parent))
      [flow])))

(defn execute-remove-flow-tag!
  [{:keys [flow-id tag] :as action}]
  (let [flow-ancestors (->> (flow-ancestors flow-id)
                            (map #(remove-tag % tag))
                            (map (juxt :flow-id identity))
                            (into {}))]
    (swap! flows merge flow-ancestors)
    (dispatch-success-fn action)))

(defn flow-success!
  [flow-id action-id]
  (when flow-id
    (let [succeeded (get-in @flows [flow-id :succeeded] #{})]
      (swap! flows assoc-in [flow-id :succeeded] (conj succeeded action-id))
      (check-flow! flow-id))))

(defn execute-remove-flows!
  [{:keys [flow-tag] :as action}]
  (let [flows-to-remove (->> @flows
                             (filter (fn [[k v]] (contains? (:tags v) flow-tag)))
                             (map second))
        flows-filtered (->> @flows
                            (filter (fn [[k v]] (not (contains? (:tags v) flow-tag))))
                            (into {}))]
    (doseq [flow flows-to-remove
            handler (:on-remove flow)]
      (handler))
    (reset! flows flows-filtered)
    (dispatch-success-fn action)))

(re-frame/reg-event-fx
  ::execute-remove-flows
  (fn [{:keys [db]} [_ action]]
    "Execute `remove-flows` action - terminate execution of actions marked with passed tag.
    Action supposed to be terminated should contain parameter `:tags`:
    {:type     'action',
     :tags     ['instruction']
     :from-var [{:var-name 'current-concept', :var-property 'game-voice-action'}]}

    Action params:
    :flow-tag - tag name.

    Example:
    {:type     'remove-flows',
     :flow-tag 'instruction'}"
    (execute-remove-flows! action)
    {}))

(re-frame/reg-event-fx
  ::execute-remove-flow-tag
  (fn [{:keys [db]} [_ action]]
    "Execute `remove-flow-tag` action - remove a tag from an action.
    Can be useful in case of `:unique-tag` parameter using. Only one action with specific `:unique-tag` can be run at the same time.

    Action params:
    :tag - tag name.

    Example:
    {:type 'remove-flow-tag',
     :tag  'clickable'}"
    (execute-remove-flow-tag! action)
    {}))

(re-frame/reg-event-fx
  ::flow-success
  (fn [{:keys [db]} [_ flow-id action-id]]
    (flow-success! flow-id action-id)
    {}))

(re-frame/reg-event-fx
  ::execute-sequence
  [event-as-action with-vars]
  (fn-traced [{:keys [db]} action]
    "Execute `sequence` action - run a sequence of actions defined by their names.

    Action params:
    :data - actions names vector.

    Example:
    {:type        'sequence',
     :data        ['start-activity' 'clear-instruction' 'reset-tools' 'init-current-tool']}"
    (let [data (->> (:data action)
                    (map #(get-action % db action))
                    (into []))]
      {:dispatch [::execute-sequence-data (assoc action :data data :type "sequence-data")]})))

(defn execute-sequence-data!
  [db action]
  (if (empty? (:data action))
    (dispatch-success-fn action)
    (let [action (->with-vars db action)
          [current & rest] (:data action)
          sequence-skippable? (:skippable action)
          skippable? (:skippable current)
          rest (if skippable? (into [{:type "hide-skip"}] rest) rest)
          next #(execute-sequence-data! db (-> action (assoc :data rest)))
          flow-id (random-uuid)
          action-id (random-uuid)
          current-scene (:current-scene db)
          flow-data {:flow-id       flow-id :actions [action-id] :type :all :next next :parent (:flow-id action) :tags (get-action-tags action)
                     :current-scene current-scene}
          current-action (-> current
                             (assoc :flow-id flow-id)
                             (assoc :action-id action-id)
                             (with-prev action))]
      (register-flow! flow-data)

      (when skippable?
        (on-skip! #(dispatch-success-fn current-action))
        (re-frame/dispatch [::overlays/show-skip-menu]))

      (when sequence-skippable?
        (on-skip! (:on-skip action))
        (on-skip! #(execute-remove-flows! {:flow-tag "skip"})))

      (execute-action db current-action))))

(re-frame/reg-event-fx
  ::execute-sequence-data
  [event-as-action]
  (fn-traced [{:keys [db]} action]
    "Execute `sequence-data` action - run a sequence of actions defined by their data.

    Action params:
    :data - actions data vector.

    Example:
    {:type 'sequence-data',
     :data [{:type 'animation', :id 'volley_call', :target 'vera'}
            {:type 'add-animation', :id 'volley_idle', :target 'vera', :loop true}]}"
    (execute-sequence-data! db action)
    {}))

(defn execute-parallel!
  [db action]
  (let [action (->with-vars db action)
        flow-id (random-uuid)
        sequence-skippable? (:skippable action)
        actions (->> (:data action)
                     (map (fn [v] (assoc v :flow-id flow-id :action-id (random-uuid))))
                     (map (fn [v] (with-prev v action))))
        action-ids (map #(get % :action-id) actions)
        current-scene (:current-scene db)
        flow-data {:flow-id       flow-id :actions action-ids :type :all
                   :parent        (:flow-id action) :tags (get-action-tags action)
                   :next          #(dispatch-success-fn action)
                   :current-scene current-scene}]
    (register-flow! flow-data)

    (when sequence-skippable?
      (on-skip! (:on-skip action))
      (on-skip! #(execute-remove-flows! {:flow-tag "skip"})))

    (if (seq actions)
      (doall (map #(execute-action db %) actions))
      (dispatch-success-fn action))))

(re-frame/reg-event-fx
  ::execute-parallel
  [event-as-action]
  (fn-traced [{:keys [db]} action]
    "Execute `parallel` action - run in parallel several actions defined by their data.

    Action params:
    :data - actions data vector.

    Example:
    {:type 'parallel',
     :data [{:type 'state', :id 'hidden', :target 'letter-trace'}
            {:type 'state', :id 'hidden', :target 'letter-tutorial-path'}]}"
    (execute-parallel! db action)
    {}))

(defn execute-callback!
  [db {:keys [callback] :as action}]
  (let [action (-> action
                   (assoc :current-scene (:current-scene db))
                   (->with-flow))]
    (when-not (nil? callback)
      (callback))
    (dispatch-success-fn action)))

(re-frame/reg-event-fx
  ::execute-callback
  [event-as-action]
  (fn [{:keys [db]} action]
    "Execute `callback` action - call external function.
    This action is used when it is generated from cljs code.

    Action params:
    :callback - a function to be called.

    Example:
    {:type     'callback'
     :callback [object Function]}"
    (execute-callback! db action)
    {}))

(re-frame/reg-event-fx
  ::execute-hide-skip
  [event-as-action with-flow]
  (fn [{:keys [db]} action]
    "Execute `hide-skip` action - hide user interface button for actions flow skipping.
    A technical function used for `:skippable` action parameter.

    Example:
    {:type 'hide-skip'}"
    (dispatch-success-fn action)
    {:dispatch [::overlays/hide-skip-menu]}))

(re-frame/reg-event-fx
  ::execute-reset-skip
  (fn [{:keys [db]} _]
    (reset! on-skip-handlers [])
    {:dispatch [::overlays/hide-skip-menu]}))

(re-frame/reg-event-fx
  ::skip
  (fn [{:keys [db]} _]
    (let [[on-skip-list _] (reset-vals! on-skip-handlers [])]
      (doseq [on-skip on-skip-list]
        (on-skip)))
    {:dispatch [::overlays/hide-skip-menu]}))
