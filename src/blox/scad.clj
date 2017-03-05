(ns blox.scad
  (:require [clojure.string :as s]))

(defn name-of [key]
  (if (or (= key :fn) (= key :fa) (= key :fs))
    (str "$" (name key))
    (name key)))

(defn val-of [val]
  (cond (or (list? val) (vector? val)) (str "[" (s/join "," (map val-of val)) "]")
        (float? val) (format "%.2f" val)
        :else (str val)))

(defn func-to-str [{:keys [name params vec] :as p}]
  (let [pms (for [[k v] (dissoc params :trans)] (str (name-of k) "=" (val-of v)))
        param-str (s/join "," pms)
        vc (when vec (val-of vec))]
    (str (if (:trans params) (str "#" name) name) "(" (if vc vc "") param-str ")")))

(defn add-param [{:keys [params] :as shape} k v]
  (assoc shape :params (assoc params k v)))

(defn func
  ([name, params]
   {:name name :params params})
  ([name, params, defaults]
   {:name name :params (merge defaults params)})
  ([name vec params defaults]
   {:name name :vec vec :params params :defaults defaults}))

(def cylinder-defaults {:h 20 :center true :fa 0.5 :fs 0.5})
(def cube-defaults {:size [20 30 40] :center true})
(def sphere-defaults {:r 20 :center true})

(defmulti primitive-gen :primitive)

(defmethod primitive-gen :sphere [{:keys [r center trans]}]
  (let [mandatory (func "sphere" {} sphere-defaults)]
    (cond-> mandatory
            trans (add-param :trans trans)
            r (add-param :r r)
            (some? center) (add-param :center center))))

(defmethod primitive-gen :cube [{:keys [size center trans] :as shape}]
  (let [mandatory
        (if size
          (func "cube" {:size size} cube-defaults)
          (func "cube" {} cube-defaults))]
    (cond-> mandatory
            trans (add-param :trans trans)
            (some? center) (add-param :center center))))

(defmethod primitive-gen :cylinder [{:keys [r r1 r2 h center fa fs fn trans] :as shape}]
  (let [mandatory
        (cond (and r r1 r2) (println "ERROR: cylinder contains r, r1, and r2 " shape)
              (and r1 r2) (func "cylinder" {:r1 r1 :r2 r2} cylinder-defaults)
              r (func "cylinder" {:r r} cylinder-defaults)
              :else (func "cylinder" {:r 20} cylinder-defaults))]
    (cond-> mandatory
            h (add-param :h h)
            fa (add-param :fa fa)
            fs (add-param :fs fs)
            fn (add-param :fn fn)
            trans (add-param :trans trans)
            (some? center) (add-param :center center))))    ;center is boolean

(defmulti operator-gen :operator)

(defmethod operator-gen :color [{:keys [vec]}] (func "color" vec {} {}))

(defmethod operator-gen :difference [_] (func "difference" {} {}))

(defmethod operator-gen :hull [_] (func "hull" {} {}))

(defmethod operator-gen :intersection [_] (func "intersection" {} {}))

(defmethod operator-gen :rotate [{:keys [vec]}] (func "rotate" vec {} {}))

(defmethod operator-gen :scale [{:keys [vec]}] (func "scale" vec {} {}))

(defmethod operator-gen :translate [{:keys [vec]}] (func "translate" vec {} {}))

(defmethod operator-gen :union [_] (func "union" {} {}))

;;(defmethod operator-gen :module [_] {:keys [params]} (func "module" {:params params})

(defn code-gen [shape]
  (cond (contains? shape :primitive) (-> shape primitive-gen func-to-str (str ";\n"))
        (contains? shape :operator) (let [sub-shapes (map code-gen (:content shape))
                                          sub-str (s/join sub-shapes)]
                                      (str (-> shape operator-gen func-to-str) "{\n" sub-str "}\n"))
        :else (println "Unknown shape " shape)))

(defn write [file shape]
  (let [file-name (if (s/ends-with? file ".scad") file (str file ".scad"))]
    (spit file-name (code-gen shape))))

;; Primitive Examples

;; A coloured arbitrary object
(defn ex0 [] (write "out/t0.scad" {:operator :color
                                   :vec      [0, 0, 1]
                                   :content  [{:operator :union
                                               :content  [{:primitive :cylinder :r 10 :h 55}
                                                          {:primitive :sphere :trans true}
                                                          {:operator :translate
                                                           :vec      [5, 5, 5]
                                                           :content  [{:primitive :cube :size [20 30 40] :trans true}]}]}]}))

;; A sphere with a hole (punched out with a translucent cylinder
(defn ex1 [] (write "out/t1.scad"
                    {:operator :difference :content [{:primitive :sphere}
                                                     {:primitive :cylinder :r 10 :h 50 :trans true}]}))

;; 3d cross
(defn ex2 [] (write "out/t2.scad"
                    {:operator :union :content [{:operator :rotate :vec [90, 0, 0] :content [{:primitive :cylinder :r 10 :h 50}]}
                                                {:operator :rotate :vec [0, 90, 0] :content [{:primitive :cylinder :r 10 :h 50}]}
                                                {:operator :rotate :vec [0, 0, 90] :content [{:primitive :cylinder :r 10 :h 50}]}]}))

; Smooth polyhedron with a circular profile in x, y, z
(defn ex3 [] (write "out/t3.scad"
                    {:operator :intersection
                     :content  [{:operator :rotate
                                 :vec      [90, 0, 0] :content [{:primitive :cylinder
                                                                 :r         10
                                                                 :h         50}]}
                                {:operator :rotate
                                 :vec      [0, 90, 0]
                                 :content  [{:primitive :cylinder :r 10 :h 50}]}
                                {:operator :rotate
                                 :vec      [0, 0, 90]
                                 :content  [{:primitive :cylinder :r 10 :h 50}]}]}))

;; Weird spaceship/submarine shape
(defn ex4 [] (write "out/t4.scad"
                    {:operator :hull
                     :content  [{:operator :scale
                                 :vec      [3, 1, 1]
                                 :content  [{:operator :rotate
                                             :vec      [90, 0, 0]
                                             :content  [{:primitive :cylinder
                                                         :r         10
                                                         :h         50}]}
                                            {:operator :rotate
                                             :vec      [0, 90, 0]
                                             :content  [{:primitive :cylinder :r 10 :h 50}]}
                                            {:operator :rotate
                                             :vec      [0, 0, 90]
                                             :content  [{:primitive :cylinder :r 10 :h 50}]}]}]}))

;; Convenience functions

(defn color  [v shape]
  {:operator :color
   :vec v
   :content  [shape]})

(defn cube [x y z]
  {:primitive :cube
   :size      [x y z]})

(defn cylinder [h r]
  {:primitive :cylinder
   :h         h
   :r         r})

(defn difference [a b & shapes]
  {:operator :difference
   :content  (concat [a b] shapes)})

(defn translate [v & shapes]
   {:operator :translate
    :vec      v
    :content  (vec shapes)})

(defn union
  ([a] {:operator :union :content (if (seq? a) a [a])})
  ([a b & shapes]
   {:operator :union
    :content  (concat [a b] shapes)}))

