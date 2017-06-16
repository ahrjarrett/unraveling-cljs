(ns unravelingcljs.advanced)

(enable-console-print!)
(println "advanced loaded")

;; Advanced Topics

;; 5.1 Transducers

;; 5.1.1 Data Transformation

(def grape-clusters
  [{:grapes [{:rotten? false :clean? false}
             {:rotten? true :clean? false}]
    :color :green}
   {:grapes [{:rotten? true :clean? false}
             {:rotten? false :clean? false}]
    :color :black}])

(defn split-cluster [c] (:grapes c))

(defn not-rotten [g]
  (not (:rotten? g)))

(defn clean-grape [g]
  (assoc g :clean? true))

(->> grape-clusters
     (mapcat split-cluster)
     (filter not-rotten)
     (map clean-grape))
;; => ({rotten? false :clean? true} {:rotten? false :clean? true})

(def process-clusters
  (comp
   (partial map clean-grape)
   (partial filter not-rotten)
   (partial mapcat split-cluster)))

(process-clusters grape-clusters)

;; This works, but it creates a lot of intermediate results that we just throw away.

;; 5.1.2 Generalizing to Process Transformations

;; First let's look under the hood of MAP and FILTER:
(defn my-map [f coll]
  (when-let [s (seq coll)]
    (cons (f (first s)) (my-map f (rest s)))))

(my-map inc [1 2 3])

(defn my-filter [pred coll]
  (when-let [s (seq coll)]
    (let [f (first s)
          r (rest s)]
      (if (pred f)
        (cons f (my-filter pred r))
        (my-filter pred r)))))

(my-filter even? [1 2 3 4 5])

;; But these are still expecting a seq, and they return a seq.

;; What if we could implement them in terms of REDUCE?
;; REDUCE can operate on a variety of diff. structures.

(defn my-mapr [f coll]
  (reduce (fn [acc curr]
            (conj acc (f curr)))
          []
          coll))

(my-mapr inc [1 2 3])

(defn my-filterr [pred coll]
  (reduce (fn [acc curr]
            (if (pred curr)
              (conj acc curr)
              acc))
          []
          coll))

(my-filterr even? [1 2 3 4 5])

;; But here we're still stuck getting back a vector,
;; Not to mention CONJ might not be the building fn
;; that we need.

;; MY-MAPT takes a fn and returns a fn that takes a reducing fn:
(defn my-mapt [f]
  (fn [rfn]
    (fn [acc curr]
      (rfn acc (f curr)))))

(def incer (my-mapt inc))

(reduce (incer conj) [] [0 1 2]) ;; => [1 2 3]

;; Playing around, got this working so easily. Cool!
(defn inc-and-reverse [coll]
  (into [] (reduce (incer conj) () coll)))

(inc-and-reverse '(9 8 7)) ;; => [8 9 10]

(defn my-filtert [pred]
  (fn [rfn]
    (fn [acc curr]
      (if (pred curr)
        (rfn acc curr)
        acc))))

(def only-odds (my-filtert odd?))

(reduce (only-odds conj) [] [0 1 2 3 4 5])
;; => (1 3 5)

;; Digging in, if I run this:
(incer conj)

;; I get back a function that looks like this:
;; (fn [acc curr]
;;   (conj acc (inc curr)))

;; "In essence, we have defined map as the transformation of a reducing function"

(reduce (incer str) "" [5 10 15 20])
;; => "6111621"

(reduce (only-odds str) "" '(3 6 9 12))
;; => "39"


;; Now let's make our own version of MAPCAT

;; MY-CAT is a transducer
(defn my-cat [rfn]
  (fn [acc curr]
    (reduce rfn acc curr)))

(reduce (my-cat conj) [] [[0 1 2] [3 4 5] [6 7 8]])
;; => [0 1 2 3 4 5 6 7 8]

(reverse (reduce (my-cat conj) '() [[0 1 2] [3 4 5] [6 7 8]]))
;; => (0 1 2 3 4 5 6 7 8)

(defn my-mapcat [f]
  (comp (my-mapt f) my-cat))

(defn dupe [x] [x x])

(def duper (my-mapcat dupe))

(reduce (duper conj) [] [1 1 2 3 5 8 13])
;; => [1 1 1 1 2 2 3 3 5 5 8 8 13 13]


;; 5.1.3 Transducers in ClojureScript Core

;; Some core fns (map, filter, mapcat) support
;; an arity 1 version that returns a transducer.

;; Let's rewrite process-grapes above:
(def new-process-clusters
  (comp
   (mapcat split-cluster)
   (filter not-rotten)
   (map clean-grape)))

;; NOTICE that the order of the args is reversed;
;; now, compose reads L->R.

(new-process-clusters grape-clusters)

;; Note that new-process-clusters is itself a transducer now.
;; "The composition of various transducers is itself a transducer."

;; Many core CLJS fns accept a transducer, for example:

(into [] new-process-clusters grape-clusters)

;; SEQUENCE:
;; (sequence xform coll)
;; When a transducer is supplied, returns a lazy sequence of applications
;; of the transform to the items in coll.
;; [It also can take multiple colls, but the transducer should accept
;; num-of-colls arguments -- not 100% sure how that would look.]
(sequence new-process-clusters grape-clusters)

(reduce (new-process-clusters conj) [] grape-clusters)

;; "Since using reduce with the reducing function returned
;; from a transducer is so common, there is a function for
;; reducing with a transformation called transduce.
;; We can now rewrite the previous call to reduce using transduce..."

;; TRANSDUCE:
;; (transduce xform f coll)
;; (transduce xform f init coll)

(transduce new-process-clusters conj [] grape-clusters)

;; 0-ARITY REQUIREMENT:
;; The reducing function returned by transducers must support
;; 0 arity. For example, (conj) returns an empty vector,
;; and so conj does support this. Do your research before blindly
;; passing the reducing fn any ol' function.


;; 5.1.5 Stateful Transducers

;; REDUCED:
;; reduced tells reduce that the reduction process should terminate.
(reduce (fn [acc curr]
          (if (= (count acc) 10)
            (reduced acc)
            (conj acc curr)))
        []
        (range 100)) ;; => [0 1 2 3 4 5 6 7 8 9]

;; TAKE:
;; (take n)
;; (take n coll)
;; Returns a lazy sequence of the first n items in coll, or all items if
;; there are fewer than n.  Returns a stateful transducer when
;; no collection is provided.

(defn my-take [n]
  (fn [rfn]
    (let [remaining (volatile! n)]
      (fn
        ([] (rfn))
        ([acc] (rfn acc))
        ([acc curr]
         (let [rem @remaining
               nr (vswap! remaining dec)
               result (if (pos? rem)
                        (rfn acc curr)
                        acc)]
           (if (not (pos? nr))
             (ensure-reduced result)
             result)))))))

(def take-five (my-take 5))
(transduce take-five conj (range 100)) ;; => [0 1 2 3 4]

;; PARTITION-ALL is different than PARTITION:
(partition 3 (range 10)) ;; => ((0 1 2) (3 4 5) (6 7 8))
(partition-all 3 (range 10)) ;; => ((0 1 2) (3 4 5) (6 7 8) (9))

;; We're going to implement out own partition-all.
;; ARRAY-LIST is a wrapper for a mutable JS array.

;; 06/15/17: This doesn't compile, yelling at me for using
;; ARRAY-LIST, which I think I need to import or require maybe?

;(defn array-list []
;  (ArrayList. (array)))

(defn my-partition-all
  [n]
  (fn [rfn]
    (let [a (array-list)]
      (fn
        ([] (rfn))
        ([result]
         (let [result (if (.isEmpty a)
                        result
                        (let [v (vec (.toArray a))]
                          (.clear a) ;; flush array contents for garbage collection
                          (unreduced (rfn result v))))]
           (rfn result)))
        ([acc input]
         (.add a input)
         (if (== n (.size a))
           (let [v (vec (.toArray a))]
             (.clear a)
             (rfn acc v))
           acc))))))

(def triples (my-partition-all 3))
(transduce triples conj (range 14))
;; => [[0 1 2] [3 4 5] [6 7 8] [9 10 11] [12 13]]


;(def frank (volatile! {:name "Frank"
;                       :surname "Smith"
;                       :occupation "Professor"
;                       :likes ["cred" "students" "new shoes"]}))



