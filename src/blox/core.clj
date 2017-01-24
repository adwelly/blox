(ns blox.core
  (:require [clojure.string :as s]))

(defn name-of [key]
  (if (or (= key :fn) (= key :fa) (= key :fs))
    (str "$" (name key))
    (name key)))

(defn val-of [val]
  (if (or (list? val) (vector? val))
    (str "[" (s/join "," val) "]")
    (str val)))

(defn func-to-str [{:keys [vec] :as p}]
  (let [params (for [[k v] (dissoc (:params p) :trans)] (str (name-of k) "=" (val-of v)))
        param-str (s/join "," params)
        vc (when vec (val-of vec))]
  (str (if (get-in p [:params :trans]) (str "#" (:name p)) (:name p)) "(" (if vc vc "") param-str ")")))

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

(defn code-gen [shape]
  (cond (contains? shape :primitive) (-> shape primitive-gen func-to-str (str ";\n"))
        (contains? shape :operator) (let [sub-shapes (map code-gen (:content shape))
                                          sub-str (s/join sub-shapes)]
                                      (str (-> shape operator-gen func-to-str) "{\n" sub-str "}\n"))
        :else (println "Unknown shape " shape)))

(defn write [file shape]
  (spit file (code-gen shape)))

