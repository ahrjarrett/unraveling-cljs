(ns unravelingcljs.basics
  (:require [clojure.string :as str]))

(enable-console-print!)

(println "basics loaded")

(def x "test")


;; SECTION 3: Language Basics

;; Variadic fns:
(defn my-variadic-set
  [& params]
  (set params))
(my-variadic-set 1 1 2 2 3 3) ;; => #{1 3 2}

;; Defining predicates in terms of sets (cool!):
(def valid? #{1 2 3 5 8 13 21})
(filter valid? (range 1 10)) ;; => (1 2 3 5 8)

;; FOR:
;; supports multiple bindings and allows for nested iterations,
;; where the innermost binding iterates "fastest":
(for [x [1 2 3]
      y [4 5]]
  [x y]) ;; => ([1 4] [1 5] [2 4] [2 5] [3 4] [3 5])

;; :LET keyword used inside FOR allows for local bindings within each iteration:
(for [x [2 3 4]
      y [5 6]
      :let [z (* x y)]]
  z) ;; => (10 12 15 18 20 24)

;; FOR with the :WHILE keyword gives us a way to break out of the iteration:
(for [x [7 8]
      y [9 10 11]
      :while (= y 9)]
  [x y]) ;; => ([7 9] [8 9])

;; FOR with the :WHEN keyword allows us to perform some powerful filtering:
;; (This is an awesome, simple simple solution to an otherwise difficult algorithm!)
(for [x [1 2 3]
      y [4 5]
      :when (= (+ x y) 6)]
  [x y]) ;; => ([1 5] [2 4])

;; Or we can combine keywords to make our intent a little clearer:
(for [x [1 2 3]
      y [4 5]
      :let [z (+ x y)]
      :when (= z 6)]
  [x y]) ;; => ([1 5] [2 4])

;; MAPs on MAPs on MAPs
;; When you use the map function on a map, the fn that is passed receives
;; two values that you can destructure representing a KEY and a VALUE, both
;; of which you can access:
(map (fn [[key value]] (* value value))
     {:ten 10 :fourteen 14 :seven 7}) ;; => (100 196 49)

;; RANGE can accept 3 args, the first is the start, second is limit,
;; and the 3rd allows you pick an interval:
(range 100 1000 100) ;; => (100 200 300 400 500 600 700 800 900)

;; TAKE-WHILE is lazy, too.
;; It accepts a predicate and an optional collection.
;; (Returns a transducer if no coll is supplied.)
(take-while (fn [x]
              (< (* 2 x x) 100))
            (range 0 100)) ;; => (0 1 2 3 4 5 6 7)


;; 3.8.3 Collections in Depth

;; LISTS

;; PEEK returns the 1st elem of a list
(def list-stack '(3 4 5))
(peek list-stack) ;; => 3

;; POP returns a list with all the elements except the first one
(pop list-stack) ;; => (4 5)

;; CONJ adds elements to the front of the list
(conj list-stack 2) ;; => (2 3 4 5)
(type (peek list-stack)) ;; => java.lang.Long
(type (pop list-stack)) ;; => clojure.lang.PersistentList


;; VECTORS

;; VEC fn allows you to pass different types of data structures
;; and make a vector outta that shit.
(vec '("Venture Bros" "Shin Chan" "Home Movies"))

;; VECTOR allows you to pass values one at a time.
(vector "Brock Sampson" "Hiro" "Brendan")

;; ASSOC works on vectors too, takes a key and value; with vectors,
;; the key is the index and the value is the (new) value at that index.
(def spanish-count (assoc ["cero" "uno" "two"] 2 "dos"))
spanish-count ;; => ["cero" "uno" "dos"]

;; Vectors can act as functions that take an index and returns its value.
;; This is really cool!
(spanish-count 0) ;; => "cero"

;; PEEK, POP and CONJ work on vectors, they just work backwards:
(def pokemon-master {:name "Ash"})
(def evolved-master
  (conj pokemon-master
        [:surname "Ketchum"] [:main-man "Pikachu"]))
(:main-man evolved-master) ;; => Pikachu

