;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dominio3.pddl 
;; Ejercicio 3D: Comunidad de 3 Hobbits + 1 Mago + 1 Elfo
;;               Objetivo: destruir el Anillo.
;;
;; AÑADIMOS precondiciones de desigualdad entre los 3 Hobbits:
;;   (not (= ?h1 ?h2))
;;   (not (= ?h1 ?h3))
;;   (not (= ?h2 ?h3))
;; para que el planner elija 3 hobbits distintos.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain dominio3)
  (:requirements :typing :adl)

  ;; ======================= 1. TIPOLOGÍA ==============================
  (:types
    Enano Hobbit Mago Elfo Humano - Personaje
    Anillo Espada Mithril Madera Mineral Especia Alimento - Recurso
    Localizacion
  )

  ;; ======================= 2. PREDICADOS =============================
  (:predicates
    (en            ?x - (either Personaje Recurso) ?l - Localizacion)
    (camino        ?l1 - Localizacion               ?l2 - Localizacion)
    (trabajando    ?p - Personaje ?l - Localizacion ?r - Recurso)
    (disponible    ?p - Personaje)

    (comunidadFormada)
    (enComunidad     ?p - Personaje)
    (lugarComunidad  ?l - Localizacion)

    (anilloRecogido  ?p - Hobbit)
    (espadaRecogida  ?p - Hobbit)
    (mithrilRecogido ?p - Hobbit)
    (anilloDestruido)
    (lugarDestruccion ?l - Localizacion)
  )

  ;; ======================= 3. ACCIONES ===============================
  ;; 3.1. ViajarIndividual 
  (:action ViajarIndividual
    :parameters (?p - Personaje
                 ?origen - Localizacion
                 ?destino - Localizacion)
    :precondition (and
      (en ?p ?origen)
      (camino ?origen ?destino)
      (disponible ?p)
      (or
        (not (comunidadFormada))
        (not (enComunidad ?p))
      )
    )
    :effect (and
      (not (en ?p ?origen))
      (en ?p ?destino)
    )
  )

  ;; 3.2. formarComunidad (3 Hobbits + 1 Mago + 1 Elfo) 
  (:action formarComunidad
    :parameters (?h1 - Hobbit
                 ?h2 - Hobbit
                 ?h3 - Hobbit
                 ?m  - Mago
                 ?e  - Elfo
                 ?l  - Localizacion)
    :precondition (and
      (disponible ?h1)
      (disponible ?h2)
      (disponible ?h3)
      (disponible ?m)
      (disponible ?e)

      (en ?h1 ?l)  (en ?h2 ?l)  (en ?h3 ?l)
      (en ?m  ?l)  (en ?e  ?l)

      ;; NUEVAS CONDICIONES DE DESIGUALDAD ENTRE HOBBITS 
      (not (= ?h1 ?h2))
      (not (= ?h1 ?h3))
      (not (= ?h2 ?h3))

      (not (comunidadFormada))
    )
    :effect (and
      (not (disponible ?h1)) (enComunidad ?h1)
      (not (disponible ?h2)) (enComunidad ?h2)
      (not (disponible ?h3)) (enComunidad ?h3)
      (not (disponible ?m))  (enComunidad ?m)
      (not (disponible ?e))  (enComunidad ?e)

      (comunidadFormada)
      (lugarComunidad ?l)
    )
  )

  ;; 3.3. viajarComunidad (3 Hobbits + 1 Mago + 1 Elfo) 
  (:action viajarComunidad
    :parameters (?origen  - Localizacion
                 ?destino - Localizacion
                 ?h1 - Hobbit
                 ?h2 - Hobbit
                 ?h3 - Hobbit
                 ?m  - Mago
                 ?e  - Elfo)
    :precondition (and
      (comunidadFormada)
      (lugarComunidad ?origen)

      (en ?h1 ?origen) (en ?h2 ?origen) (en ?h3 ?origen)
      (en ?m  ?origen) (en ?e  ?origen)

      (enComunidad ?h1) (enComunidad ?h2) (enComunidad ?h3)
      (enComunidad ?m)  (enComunidad ?e)

      (camino ?origen ?destino)
    )
    :effect (and
      (not (en ?h1 ?origen)) (en ?h1 ?destino)
      (not (en ?h2 ?origen)) (en ?h2 ?destino)
      (not (en ?h3 ?origen)) (en ?h3 ?destino)
      (not (en ?m  ?origen)) (en ?m  ?destino)
      (not (en ?e  ?origen)) (en ?e  ?destino)

      (not (lugarComunidad ?origen)) (lugarComunidad ?destino)
    )
  )

  ;; 3.4. recogerAnillo 
  (:action recogerAnillo
    :parameters (?p - Hobbit
                 ?l - Localizacion
                 ?r - Anillo)
    :precondition (and
      (comunidadFormada)
      (en ?p ?l)
      (enComunidad ?p)
      (en ?r ?l)
      (not (anilloRecogido ?p))
    )
    :effect (and
      (anilloRecogido ?p)
    )
  )

  ;; 3.5. recogerEspada 
  (:action recogerEspada
    :parameters (?p - Hobbit
                 ?l - Localizacion
                 ?r - Espada)
    :precondition (and
      (comunidadFormada)
      (en ?p ?l)
      (enComunidad ?p)
      (anilloRecogido ?p)
      (en ?r ?l)
      (not (espadaRecogida ?p))
    )
    :effect (and
      (espadaRecogida ?p)
    )
  )

  ;; 3.6. recogerMithril
  (:action recogerMithril
    :parameters (?p - Hobbit
                 ?l - Localizacion
                 ?r - Mithril)
    :precondition (and
      (comunidadFormada)
      (en ?p ?l)
      (enComunidad ?p)
      (anilloRecogido ?p)
      (en ?r ?l)
      (not (mithrilRecogido ?p))
    )
    :effect (and
      (mithrilRecogido ?p)
    )
  )

  ;; 3.7. destruirAnillo
  (:action destruirAnillo
    :parameters (?p - Hobbit
                 ?l - Localizacion)
    :precondition (and
      (anilloRecogido ?p)
      (espadaRecogida ?p)
      (mithrilRecogido ?p)
      (lugarDestruccion ?l)
      (en ?p ?l)
    )
    :effect (and
      (anilloDestruido)
    )
  )
)
