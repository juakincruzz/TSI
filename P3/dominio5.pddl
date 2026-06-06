; Practica 3 - Ejercicio 5
; Costes variables en la accion Viajar.
;
; Se parte del Ejercicio 1 y se modifica unicamente lo necesario para que
; Viajar tenga un coste no unitario segun el tramo recorrido.
;
; Mecanismo:
;   - Se declara la funcion costeCamino(?origen, ?destino) en el dominio.
;   - El problema inicializa esa funcion para cada par de localizaciones
;     conectadas con el coste indicado en el enunciado (1 por defecto,
;     valores distintos para los 15 tramos especificados).
;   - Viajar acumula (increase (total-cost) (costeCamino ?origen ?destino)).
;   - El objetivo del problema incluye (:metric minimize (total-cost)).
;   - ExtraerRecurso no modifica total-cost (contribuye 0, como exige el
;     enunciado, que solo menciona costes de desplazamiento).

(define (domain tierra-media-ej5)
  (:requirements :strips :typing :negative-preconditions :conditional-effects :action-costs)

  (:types
    Personaje Recurso Localizacion TipoPersonaje - object
  )

  (:constants
    Enano Hobbit - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
  )

  (:predicates
    (en ?p - Personaje ?l - Localizacion)
    (recursoEn ?r - Recurso ?l - Localizacion)
    (camino ?origen - Localizacion ?destino - Localizacion)
    (caminoDestructible ?origen - Localizacion ?destino - Localizacion)
    (personajeEs ?p - Personaje ?tp - TipoPersonaje)
    (puedeExtraer ?tp - TipoPersonaje ?r - Recurso)
    (puedeExtraerPersonaje ?p - Personaje ?r - Recurso)
    (disponible ?p - Personaje)
    (trabajando ?p - Personaje ?l - Localizacion ?r - Recurso)
    (recursoGenerado ?r - Recurso)
  )

  (:functions
    ; Coste total acumulado del plan (requerido por :action-costs).
    (total-cost)

    ; Coste de desplazamiento entre dos localizaciones conectadas.
    ; Se inicializa en el fichero de problema para cada arco del mapa.
    (costeCamino ?o - Localizacion ?d - Localizacion)
  )

  ; ---------------------------------------------------------------------------
  ; Viajar
  ; ---------------------------------------------------------------------------
  ; Identica al Ejercicio 1 salvo por el efecto de coste: acumula en
  ; total-cost el valor de costeCamino(?origen, ?destino), que el problema
  ; fija con el coste real de cada tramo.
  (:action Viajar
    :parameters (?p - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (disponible ?p)
        (en ?p ?origen)
        (camino ?origen ?destino)
      )
    :effect
      (and
        (not (en ?p ?origen))
        (en ?p ?destino)
        (increase (total-cost) (costeCamino ?origen ?destino))
        (when (caminoDestructible ?origen ?destino)
          (and
            (not (camino ?origen ?destino))
            (not (camino ?destino ?origen))
          )
        )
      )
  )

  ; ---------------------------------------------------------------------------
  ; ExtraerRecurso
  ; ---------------------------------------------------------------------------
  ; Sin cambios respecto al Ejercicio 1. No modifica total-cost (coste 0).
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and
        (disponible ?p)
        (en ?p ?l)
        (recursoEn ?r ?l)
        (puedeExtraerPersonaje ?p ?r)
      )
    :effect
      (and
        (not (disponible ?p))
        (trabajando ?p ?l ?r)
        (recursoGenerado ?r)
      )
  )
)
