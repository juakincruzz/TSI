;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dominio para el Ejercicio 1: “Modelo base”
;;
;; Requisitos mínimos:
;;  - 3 tipos: Personaje, Recurso, Localizacion
;;  - Subtipos: Enano, Hobbit (heredando de Personaje)
;;              Mineral, Mithril, Madera, Especia, Alimento (heredando de Recurso)
;;  - 4 predicados: en, camino, trabajando, disponible
;;  - 2 acciones obligatorias: Viajar y ExtraerRecurso
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain dominio1)
  (:requirements :typing)

  (:types
    Enano Hobbit - Personaje
    Mineral Mithril Madera Especia Alimento - Recurso
    Localizacion
  )

  (:predicates
    (en         ?x - (either Personaje Recurso) ?l - Localizacion)
    (camino     ?l1 - Localizacion               ?l2 - Localizacion)
    (trabajando ?p - Personaje ?l - Localizacion ?r - Recurso)
    (disponible ?p - Personaje)
  )

  ;; ===================================================================
  ;; Acción: Viajar 
  ;; ===================================================================
  (:action Viajar
    :parameters (?p - Personaje
                 ?origen - Localizacion
                 ?destino - Localizacion)
    :precondition (and
      (en ?p ?origen)
      (camino ?origen ?destino)
      (disponible ?p)
    )
    :effect (and
      (not (en ?p ?origen))
      (en ?p ?destino)
    )
  )

  ;; ===================================================================
  ;; Acción: ExtraerRecurso (solo los Enanos pueden extraer)
  ;; ===================================================================
  (:action ExtraerRecurso
    :parameters (?p - Enano
                 ?l - Localizacion
                 ?r - Recurso)
    :precondition (and
      (en ?p ?l)
      (disponible ?p)
      (en ?r ?l)
    )
    :effect (and
      (not (disponible ?p))
      (trabajando ?p ?l ?r)
    )
  )
)
