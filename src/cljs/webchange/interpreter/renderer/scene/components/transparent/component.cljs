(ns webchange.interpreter.renderer.scene.components.transparent.component
  (:require
    [webchange.interpreter.renderer.pixi :refer [Container Sprite Texture]]
    [webchange.interpreter.renderer.scene.components.transparent.wrapper :refer [wrap]]
    [webchange.interpreter.renderer.scene.components.utils :as utils]))

(def default-props {:x        {}
                    :y        {}
                    :width    {}
                    :height   {}
                    :name     {}
                    :on-click {}
                    :ref      {}
                    :scale    {:default {:x 1 :y 1}}})

(defn- create-container
  [{:keys [x y scale]}]
  (doto (Container.)
    (utils/set-position {:x x :y y})
    (utils/set-scale scale)))

(defn- create-sprite
  [{:keys [width height]}]
  (doto (Sprite. (.-EMPTY Texture))
    (utils/set-size {:width  width
                     :height height})))

(def component-type "transparent")

(defn create
  [parent {:keys [type object-name on-click ref  ] :as props}]
  (let [container (create-container props)
        sprite (create-sprite props)
        wrapped-container (wrap type object-name container)]

    (when on-click (utils/set-handler container "click" on-click))
    (when ref (ref wrapped-container))

    (.addChild container sprite)
    (.addChild parent container)

    wrapped-container))
