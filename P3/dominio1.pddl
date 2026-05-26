; Practica 3 - Ejercicio 1
; Dominio base de planificacion para la Tierra Media.
;
; Idea de modelado:
; - Los personajes, recursos y localizaciones son objetos tipados.
; - Enano y Hobbit se representan como constantes de tipo TipoPersonaje,
;   no como tipos PDDL, tal como pide el enunciado.
; - Los recursos tambien son constantes del dominio, porque representan
;   clases de recurso, no unidades concretas de mineral o alimento.

(define (domain tierra-media-ej1)
  (:requirements :strips :typing :negative-preconditions :conditional-effects)

  (:types
    Personaje Recurso Localizacion TipoPersonaje - object
  )

  (:constants
    Enano Hobbit - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
  )

  (:predicates
    ; Localizacion actual de cada personaje.
    (en ?p - Personaje ?l - Localizacion)

    ; Nodo de recurso disponible en una localizacion.
    (recursoEn ?r - Recurso ?l - Localizacion)

    ; Camino dirigido entre localizaciones. En el problema se introducen
    ; ambos sentidos de cada arista del mapa.
    (camino ?origen - Localizacion ?destino - Localizacion)

    ; Algunos caminos desaparecen tras usarse. En este ejercicio se usa para
    ; Rivendell-Moria, pero se deja como dato del problema.
    (caminoDestructible ?origen - Localizacion ?destino - Localizacion)

    ; Tipo asignado a un personaje concreto.
    (personajeEs ?p - Personaje ?tp - TipoPersonaje)

    ; Capacidad general por tipo de personaje, pedida por el enunciado.
    (puedeExtraer ?tp - TipoPersonaje ?r - Recurso)

    ; Capacidad ya instanciada por personaje. Evita cuantificadores en
    ; ExtraerRecurso y mantiene la accion con los tres parametros exigidos.
    (puedeExtraerPersonaje ?p - Personaje ?r - Recurso)

    ; Un personaje disponible puede viajar o ser asignado a una extraccion.
    (disponible ?p - Personaje)

    ; Un personaje queda trabajando en una localizacion extrayendo un recurso.
    (trabajando ?p - Personaje ?l - Localizacion ?r - Recurso)

    ; Predicado auxiliar para expresar el objetivo por tipo de recurso.
    (recursoGenerado ?r - Recurso)
  )

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
        (when (caminoDestructible ?origen ?destino)
          (and
            (not (camino ?origen ?destino))
            (not (camino ?destino ?origen))
          )
        )
      )
  )

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
