(ns webchange.dashboard.students.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::class-students
  (fn [db [_ id]]
    (get-in db [:dashboard :students id])))

(re-frame/reg-sub
  ::current-student-id
  (fn [db]
    (get-in db [:dashboard :current-student-id])))

(re-frame/reg-sub
  ::current-student
  (fn [db]
    (get-in db [:dashboard :current-student])))

(re-frame/reg-sub
  ::generated-code
  (fn [db]
    (get-in db [:dashboard :access-code])))

(re-frame/reg-sub
  ::student-modal-state
  (fn [db]
    (get-in db [:dashboard :student-modal-state])))

(re-frame/reg-sub
  ::students-loading
  (fn [db]
    (get-in db [:loading :students])))

(re-frame/reg-sub
  ::student-loading
  (fn [db]
    (get-in db [:loading :student])))
