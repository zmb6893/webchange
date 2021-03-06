(ns webchange.editor-v2.graph-builder.graph.data.writing-practice.source)

(def data {:assets
                     [{:url "/raw/img/library/painting-tablet/background.jpg", :type "image"}
                      {:url "/raw/img/ui/back_button_01.png", :type "image"}
                      {:url "/raw/audio/l2/a7/L2_A7_Mari.m4a", :type "audio", :alias "mari voice"}
                      {:url "/raw/img/elements/squirrel.png", :type "image"}],
           :objects
                     {:background    {:type "background", :scene-name "background", :src "/raw/img/library/painting-tablet/background.jpg"},
                      :concept-card  {:type "group", :x 60, :y 30, :width 400, :height 300, :children ["concept-image" "concept-word"]},
                      :concept-image {:type "image", :x 100, :y 0, :width 100, :height 100, :rotation 0, :scale-x 2, :scale-y 2},
                      :concept-word
                                     {:type           "text",
                                      :x              0,
                                      :y              190,
                                      :width          400,
                                      :height         100,
                                      :align          "center",
                                      :fill           "black",
                                      :font-family    "Lexend Deca",
                                      :font-size      80,
                                      :scale-x        1,
                                      :scale-y        1,
                                      :text           "",
                                      :vertical-align "middle"},
                      :copybook
                                     {:type           "copybook",
                                      :x              60,
                                      :y              0,
                                      :width          1800,
                                      :height         1050,
                                      :scene-name     "copybook",
                                      :line-height    180,
                                      :padding-height 90,
                                      :rotation       0,
                                      :scale-x        1,
                                      :scale-y        1},
                      :letter-trace
                                     {:type         "svg-path",
                                      :x            600,
                                      :y            115,
                                      :scene-name   "letter-trace",
                                      :dash         [7 7],
                                      :data         "",
                                      :line-cap     "round",
                                      :rotation     0,
                                      :scale-x      3.2,
                                      :scale-y      3.2,
                                      :states       {:hidden {:type "transparent"}, :visible {:type "svg-path"}},
                                      :stroke       "#898989",
                                      :stroke-width 4},
                      :letter-trace-matrix
                                     {:type "matrix", :x -70, :y 330, :width 2025, :height 675, :dx 675, :dy 360, :el "letter-trace", :max 3},
                      :letter-tutorial-image
                                     {:type "image", :x 965, :y 75, :width 651, :height 651, :rotation 0, :scale-x 0.45, :scale-y 0.45},
                      :letter-tutorial-path
                                     {:type         "transparent",
                                      :x            500,
                                      :y            25,
                                      :width        225,
                                      :height       225,
                                      :scene-name   "letter-path",
                                      :animation    "stop",
                                      :duration     5000,
                                      :fill         "transparent",
                                      :line-cap     "round",
                                      :path         "",
                                      :scale-x      1.8,
                                      :scale-y      1.8,
                                      :states       {:hidden {:type "transparent"}, :visible {:type "animated-svg-path"}},
                                      :stroke       "#323232",
                                      :stroke-width 10},
                      :letter-tutorial-trace
                                     {:type         "svg-path",
                                      :x            500,
                                      :y            25,
                                      :width        225,
                                      :height       225,
                                      :scene-name   "letter-tutorial-trace",
                                      :dash         [7 7],
                                      :data         "",
                                      :line-cap     "round",
                                      :rotation     0,
                                      :scale-x      1.8,
                                      :scale-y      1.8,
                                      :states       {:hidden {:type "transparent"}, :visible {:type "svg-path"}},
                                      :stroke       "#898989",
                                      :stroke-width 4},
                      :mari
                                     {:type       "animation",
                                      :x          1700,
                                      :y          520,
                                      :width      473,
                                      :height     511,
                                      :scene-name "mari",
                                      :transition "mari",
                                      :anim       "idle",
                                      :name       "mari",
                                      :scale-x    0.5,
                                      :scale-y    0.5,
                                      :speed      0.35,
                                      :start      true},
                      :next-button
                                     {:type     "image",
                                      :x        1875,
                                      :y        915,
                                      :width    97,
                                      :height   99,
                                      :actions  {:click {:id "go-next", :on "click", :type "action"}},
                                      :rotation 0,
                                      :scale-x  -1,
                                      :scale-y  1,
                                      :src      "/raw/img/ui/back_button_01.png"},
                      :practice-canvas
                                     {:type       "painting-area",
                                      :x          0,
                                      :y          0,
                                      :width      1920,
                                      :height     1080,
                                      :scene-name "practice-canvas",
                                      :color      "#323232",
                                      :rotation   0,
                                      :scale-x    1,
                                      :scale-y    1,
                                      :states     {:hidden {:type "transparent"}, :visible {:type "painting-area"}},
                                      :tool       "brush",
                                      :var-name   "writing-practice-stage-1-pt-1"}},
           :scene-objects
                     [["background" "concept-card"]
                      ["letter-trace-matrix" "letter-tutorial-trace" "letter-tutorial-path" "letter-tutorial-image"]
                      ["practice-canvas" "next-button" "mari"]],
           :actions
                     {:check-page
                                      {:type     "test-var-scalar",
                                       :var-name "current-page",
                                       :success  "check-stage",
                                       :fail     "go-next-page",
                                       :from-var [{:var-name "pages-number", :action-property "value"}]},
                      :check-stage
                                      {:type "test-var-scalar", :var-name "current-stage", :value 2, :success "finish-activity", :fail "go-next-stage"},
                      :finish-activity
                                      {:type "sequence-data",
                                       :dat  [{:id "writing-practice", :type "finish-activity"} {:type "scene", :scene-id "library"}]},
                      :go-next        {:type "action", :id "check-page"},
                      :go-next-page
                                      {:type "sequence-data",
                                       :data
                                             [{:type "counter", :counter-id "current-page", :counter-action "increase"}
                                              {:id "update-painting-var", :type "action"}]},
                      :go-next-stage
                                      {:type "sequence-data",
                                       :data
                                             [{:type "counter", :counter-id "current-stage", :counter-action "increase"}
                                              {:type "counter", :counter-id "current-page", :counter-value 0, :counter-action "reset"}
                                              {:id "go-next-page", :type "action"}
                                              {:id "hide-stage-1", :type "action"}
                                              {:id "init-stage-2", :type "action"}
                                              {:id "mari-voice-intro-2", :type "action"}]},
                      :hide-stage-1
                                      {:type "parallel",
                                       :data
                                             [{:id "hidden", :type "state", :target "letter-trace"}
                                              {:id "hidden", :type "state", :target "letter-tutorial-path"}]},
                      :init-stage-1
                                      {:type "parallel",
                                       :data
                                             [{:type      "set-attribute",
                                               :target    "letter-trace",
                                               :from-var  [{:var-name "letter-path", :action-property "attr-value"}],
                                               :attr-name "data"}
                                              {:type      "set-attribute",
                                               :target    "letter-tutorial-trace",
                                               :from-var  [{:var-name "letter-path", :action-property "attr-value"}],
                                               :attr-name "data"}
                                              {:type      "set-attribute",
                                               :target    "letter-tutorial-path",
                                               :from-var  [{:var-name "letter-path", :action-property "attr-value"}],
                                               :attr-name "path"}]},
                      :init-stage-2
                                      {:type "parallel",
                                       :data
                                             [{:type      "set-attribute",
                                               :target    "letter-tutorial-1",
                                               :from-var  [{:var-name "letter-tutorial-path", :action-property "attr-value"}],
                                               :attr-name "data"}
                                              {:type      "set-attribute",
                                               :target    "letter-tutorial-2",
                                               :from-var  [{:var-name "letter-tutorial-path", :action-property "attr-value"}],
                                               :attr-name "data"}
                                              {:type      "set-attribute",
                                               :target    "letter-tutorial-3",
                                               :from-var  [{:var-name "letter-tutorial-path", :action-property "attr-value"}],
                                               :attr-name "data"}
                                              {:id "visible", :type "state", :target "letter-tutorial-group"}]},
                      :init-state
                                      {:type "parallel",
                                       :data
                                             [{:type      "set-attribute",
                                               :target    "concept-image",
                                               :from-var  [{:var-name "current-concept", :var-property "image-src", :action-property "attr-value"}],
                                               :attr-name "src"}
                                              {:type      "set-attribute",
                                               :target    "concept-word",
                                               :from-var  [{:var-name "current-concept", :var-property "concept-name", :action-property "attr-value"}],
                                               :attr-name "text"}
                                              {:type      "set-attribute",
                                               :target    "letter-tutorial-image",
                                               :from-var  [{:var-name "current-concept", :var-property "letter-src", :action-property "attr-value"}],
                                               :attr-name "src"}]},
                      :init-vars
                                      {:type "parallel",
                                       :data
                                             [{:type "set-variable", :var-name "current-stage", :var-value 1}
                                              {:type "set-variable", :var-name "current-page", :var-value 1}
                                              {:type "calc", :value-1 30, :value-2 13, :var-name "pages-number", :operation "div-ceil"}
                                              {:type     "set-variable",
                                               :from-var [{:var-name "current-concept", :var-property "letter-path", :action-property "var-value"}],
                                               :var-name "letter-path"}
                                              {:type     "set-variable",
                                               :from-var [{:var-name "current-concept", :var-property "letter-tutorial-path", :action-property "var-value"}],
                                               :var-name "letter-tutorial-path"}]},
                      :mari-go-wait   {:type "transition", :to {:x 1490, :y 130, :loop false, :duration 2}, :transition-id "mari"},
                      :mari-init-wand {:type "add-animation", :id "wand_idle", :target "mari", :track 2, :loop true},
                      :mari-shows-example
                                      {:type "sequence-data",
                                       :data
                                             [{:to {:x 980, :y 360, :loop false, :duration 1.5}, :type "transition", :transition-id "mari"}
                                              {:id "visible", :type "state", :target "letter-tutorial-path"}
                                              {:data
                                                     [{:type "path-animation", :state "play", :target "letter-tutorial-path"}
                                                      {:to            {:path "", :scale {:x 1.7, :y 1.7}, :origin {:x 680, :y 60}, :duration 5},
                                                       :type          "transition",
                                                       :from-var      [{:var-name "letter-path", :action-property "to.path"}],
                                                       :transition-id "mari"}],
                                               :type "parallel"}
                                              {:type "set-attribute", :target "mari", :attr-name "scale-x", :attr-value -0.5}
                                              {:to {:x 1490, :y 180, :loop false, :duration 2}, :type "transition", :transition-id "mari"}]},
                      :mari-voice-go-next-practice
                                      {:type               "sequence-data"
                                       :phrase             :next-practice
                                       :phrase-description "Next practice"
                                       :data               [{:offset                 58.908,
                                                             :phrase-text            "Good job tracing the letter a!"
                                                             :phrase-text-translated "Buen trabajo rastreando la letra",
                                                             :start                  58.908,
                                                             :type                   "animation-sequence",
                                                             :duration               2.637,
                                                             :volume                 0.2,
                                                             :audio                  "/raw/audio/l2/a7/L2_A7_Mari.m4a",
                                                             :target                 "mari",
                                                             :track                  1,
                                                             :data                   [{:end 62.529, :anim "talk", :start 59.223}]}
                                                            {:offset                 62.923,
                                                             :phrase-text            "Click on the arrow to do a little more writing work."
                                                             :phrase-text-translated "Haz click en la flecha para seguir con tu trabajo.",
                                                             :start                  62.923,
                                                             :type                   "animation-sequence",
                                                             :duration               4.054,
                                                             :volume                 0.2,
                                                             :audio                  "/raw/audio/l2/a7/L2_A7_Mari.m4a",
                                                             :target                 "mari",
                                                             :track                  1,
                                                             :data                   [{:end 66.859, :anim "talk", :start 63.08}]}]}
                      :mari-voice-intro
                                      {:offset                 32.357,
                                       :phrase                 :welcome
                                       :phrase-description     "Welcome"
                                       :phrase-text            "Pick a pencil or a color and follow the arrows to trace the letter.  Remember to follow the correct tracing pattern.  I’ll show you how to do the first one."
                                       :phrase-text-translated "Escoge un lapiz o un color y sigue las flechas para rastrear la letra. Recuerda seguir el modelo correcto de rastrear. Yo te ensenare como hacer el primero. ",
                                       :start                  32.357,
                                       :type                   "animation-sequence",
                                       :duration               17.005,
                                       :volume                 0.2,
                                       :audio                  "/raw/audio/l2/a7/L2_A7_Mari.m4a",
                                       :target                 "mari",
                                       :track                  1,
                                       :data                   [{:end 36.254, :anim "talk", :start 33.026}
                                                                {:end 40.367, :anim "talk", :start 36.707}
                                                                {:end 45.485, :anim "talk", :start 41.627}
                                                                {:end 49.047, :anim "talk", :start 46.193}]},
                      :mari-voice-intro-2
                                      {:offset                 70.402,
                                       :phrase                 :free-drawing
                                       :phrase-description     "Free drawing intro"
                                       :phrase-text            "On this page, you should practice drawing the letters OR you can draw anything you want. When you are finished drawing, click the arrow to go to your next activity.",
                                       :phrase-text-translated "En esta pagina, podras practicar a dibujar las letras O podras dibujar lo que gustes. Cuando termines, haz click sobre la flecha para ir a tu proxima actividad.",
                                       :start                  70.402,
                                       :type                   "animation-sequence",
                                       :duration               16.848,
                                       :volume                 0.2,
                                       :audio                  "/raw/audio/l2/a7/L2_A7_Mari.m4a",
                                       :target                 "mari",
                                       :track                  1,
                                       :data                   [{:end 75.952, :anim "talk", :start 70.677}
                                                                {:end 79.298, :anim "talk", :start 76.68}
                                                                {:end 81.483, :anim "talk", :start 80.046}
                                                                {:end 86.915, :anim "talk", :start 82.073}]},
                      :mari-voice-try-again
                                      {:offset                 52.432,
                                       :phrase                 :more-practice
                                       :phrase-description     "More practice"
                                       :phrase-text            "Touch here for more tracing practice",
                                       :phrase-text-translated "Toca aqui para seguir rastreando",
                                       :start                  52.432,
                                       :type                   "animation-sequence",
                                       :duration               3.228,
                                       :volume                 0.2,
                                       :audio                  "/raw/audio/l2/a7/L2_A7_Mari.m4a",
                                       :target                 "mari",
                                       :track                  1,
                                       :data                   [{:end 55.562, :anim "talk", :start 52.629}]},
                      :renew-concept
                                      {:type "lesson-var-provider", :from "concepts-single", :provider-id "concepts", :variables ["current-concept"]},
                      :start-scene
                                      {:type "sequence",
                                       :data
                                             ["start-activity"
                                              "renew-concept"
                                              "init-state"
                                              "init-vars"
                                              "init-stage-1"
                                              "mari-voice-intro"
                                              "mari-init-wand"
                                              "mari-shows-example"
                                              "mari-voice-go-next-practice"]},
                      :start-activity {:type "start-activity", :id "writing-practice"},
                      :stop-activity  {:type "stop-activity", :id "writing-practice"},
                      :update-painting-var
                                      {:type     "test-var-scalar",
                                       :var-name "current-stage",
                                       :value    1,
                                       :success
                                                 {:type      "set-attribute",
                                                  :target    "practice-canvas",
                                                  :from-var  [{:template "writing-practice-stage-1-pt-%", :var-name "current-page", :action-property "attr-value"}],
                                                  :attr-name "var-name"},
                                       :fail
                                                 {:type      "set-attribute",
                                                  :target    "practice-canvas",
                                                  :from-var  [{:template "writing-practice-stage-2-pt-%", :var-name "current-page", :action-property "attr-value"}],
                                                  :attr-name "var-name"}}},
           :triggers {:stop {:on "back", :action "stop-activity"}, :start {:on "start", :action "start-scene"}},
           :metadata {:prev "library", :autostart true}})
