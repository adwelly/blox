(ns blox.brick
  (:require [blox.scad :refer [write cube difference translate]]))

(def iota 0.2)
(def brick-dim 7.8)
(def half-brick-dim (* brick-dim 0.5))
(def tile-height 3.21)
(def brick-height (* tile-height 3))
(def half-brick-height (* brick-height 0.5))
(def stud-height 1.77)
(def half-stud-height (* stud-height 0.5))
(def stud-radius 2.45)
(def wall-depth 1)
(def buttress-height 5)
(def half-buttress-height (* buttress-height 0.5))
(def buttress-width 0.84)
(def tube-height (- brick-height iota))
(def tube-radius 3.25)

(defn studded-cube [l w h stud-h stud-r]
  {:operator :color
   :vec      [1 0 0]
   :content  [{:operator :union
               :content  [(cube l w h)
                          {:operator :translate
                           :vec      [0 0 half-stud-height]
                           :content  [{:primitive :cylinder :h (+ h stud-h) :r stud-r}]}]}]})

(defn stud1 [h]
  (studded-cube brick-dim brick-dim h stud-height stud-radius))

(defn solid [h m n]
  (let [row (for [i (range m)] {:operator :translate :vec [(* i brick-dim) 0 0] :content [(stud1 h)]})
        cols (for [j (range n)] {:operator :translate :vec [0 (* j brick-dim) 0] :content row})]
    (translate [(+ half-brick-dim (* -1 half-brick-dim m)) (+ half-brick-dim (* -1 half-brick-dim n)) 0] {:operator :union :content cols})))


(defn buttress [studs]
  (cube buttress-width (- (* studs brick-dim) iota) buttress-height))

(defn tube []
  {:operator :difference
   :content  [{:primitive :cylinder :h tube-height :r tube-radius}
              {:operator :translate
               :vector   [0 0 iota]
               :content  [{:primitive :cylinder :h (+ tube-height iota) :r (- tube-radius buttress-width)}]}]})

(defn interior [m n]
  (let [half-total-width (* m half-brick-dim)
        half-total-length (* n half-brick-dim)
        buttresses (for [i (range (dec m))] {:operator :translate :vec [(- (* (inc i) brick-dim) half-total-width) 0 (- half-brick-height half-buttress-height iota)] :content [(buttress n)]})
        tubes (for [i (range (dec m)) j (range (dec n))] {:operator :translate :vec [(- (* (inc i) brick-dim) half-total-width)
                                                                                     (- (* (inc j) brick-dim) half-total-length)
                                                                                     0] :content [(tube)]})]
    {:operator :union
     :content  (concat buttresses tubes)}))

(defn plate [m n]
  (solid tile-height m n))

(defn brick [m n]
  {:operator :union
   :content  [(interior m n)
              {:operator :difference
               :content  [(solid brick-height m n)
                          (translate [0 0 (* -1 wall-depth)] (cube (- (* m brick-dim) wall-depth) (- (* n brick-dim) wall-depth) brick-height))]}]})

(defn make []
  (write "out/stud1" (brick 3 4)))

