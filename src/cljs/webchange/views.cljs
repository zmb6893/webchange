(ns webchange.views
  (:require
   [re-frame.core :as re-frame]
   [webchange.subs :as subs]
   [webchange.interpreter.components :refer [course]]
   ))

(defn main-panel []
  [course "test-course"])