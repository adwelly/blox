(ns blox.brick
  (:require [blox.scad :refer [write]]))

(def brick-dim 7.8)
(def tile-height 3.21)
(def brick-height (* tile-height 3))
(def stud-height 1.77)
(def stud-radius 2.45)

(defn studded-cube [l w h stud-h stud-r]
  {:operator :color
   :vec      [1 0 0]
   :content  [{:operator :union
               :content  [{:primitive :cube :size [l w h]}
                          {:operator :translate
                           :vec      [0 0 (/ stud-height 2)]
                           :content  [{:primitive :cylinder :h (+ h stud-h) :r stud-r}]}]}]})

(defn stud1 [h]
  (studded-cube brick-dim brick-dim h stud-height stud-radius))

(defn solid [h m n]
  (let [row (for [i (range m)] {:operator :translate :vec [(* i brick-dim) 0 0] :content [(stud1 h)]})
        cols (for [j (range n)] {:operator :translate :vec [0 (* j brick-dim) 0] :content row})]
    {:operator :translate
     :vec [(+ (/ brick-dim 2.0) (* -1 brick-dim (/ m 2.0))) (+ (/ brick-dim 2.0) (* -1 brick-dim (/ n 2.0))) 0]
     :content [{:operator :union :content cols}]}))

(defn plate [m n]
  (solid tile-height m n))

(defn brick [m n]
  (solid brick-height m n))

(defn make []
  (write "out/stud1" (brick 7 9)))
