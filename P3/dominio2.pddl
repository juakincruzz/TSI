; Practica 3 - Ejercicio 2
; Extension del dominio del Ejercicio 1 para formar una Comunidad reducida,
; recoger el Anillo, el Chaleco de Mithril y la Espada, y destruir el Anillo.

(define (domain tierra-media-ej2)
  (:requirements :strips :typing :negative-preconditions :conditional-effects)

  (:types
    Personaje Recurso Localizacion TipoPersonaje Objeto - object
  )

  (:constants
    Enano Hobbit Mago - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
    Anillo ChalecoMithril Espada - Objeto
  )

  (:predicates
    ; Predicados heredados del Ejercicio 1.
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
    (recursoGeneradoEn ?r - Recurso ?l - Localizacion)

    ; Comunidad reducida: un Hobbit y un Mago.
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)

    ; Objetos necesarios para destruir el Anillo.
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)

    ; La localizacion de destruccion se define en el problema, no en el dominio.
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)
  )

  (:action Viajar
    :parameters (?p - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (disponible ?p)
        (not (miembroComunidad ?p))
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
        (recursoGeneradoEn ?r ?l)
      )
  )

  (:action formarComunidad
    :parameters (?hobbit - Personaje ?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (comunidadFormada))
        (disponible ?hobbit)
        (disponible ?mago)
        (personajeEs ?hobbit Hobbit)
        (personajeEs ?mago Mago)
        (en ?hobbit ?l)
        (en ?mago ?l)
      )
    :effect
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit)
        (miembroComunidad ?mago)
      )
  )

  (:action viajarComunidad
    :parameters (?hobbit - Personaje ?mago - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit)
        (miembroComunidad ?mago)
        (personajeEs ?hobbit Hobbit)
        (personajeEs ?mago Mago)
        (en ?hobbit ?origen)
        (en ?mago ?origen)
        (camino ?origen ?destino)
      )
    :effect
      (and
        (not (en ?hobbit ?origen))
        (not (en ?mago ?origen))
        (en ?hobbit ?destino)
        (en ?mago ?destino)
        (when (caminoDestructible ?origen ?destino)
          (and
            (not (camino ?origen ?destino))
            (not (camino ?destino ?origen))
          )
        )
      )
  )

  (:action materializarChaleco
    :parameters (?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (chalecoMaterializado))
        (comunidadFormada)
        (miembroComunidad ?mago)
        (personajeEs ?mago Mago)
        (en ?mago ?l)
        (recursoGeneradoEn Mithril ?l)
      )
    :effect
      (and
        (chalecoMaterializado)
        (objetoEn ChalecoMithril ?l)
      )
  )

  (:action recogerObjeto
    :parameters (?p - Personaje ?l - Localizacion ?o - Objeto)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?p)
        (en ?p ?l)
        (objetoEn ?o ?l)
        (puedeRecogerObjeto ?p ?o)
      )
    :effect
      (and
        (not (objetoEn ?o ?l))
        (tieneObjeto ?p ?o)
        (when (objetoEsAnillo ?o)
          (and
            (portadorAnillo ?p)
            (puedeRecogerObjeto ?p ChalecoMithril)
            (puedeRecogerObjeto ?p Espada)
          )
        )
      )
  )

  (:action destruirAnillo
    :parameters (?p - Personaje ?l - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?p)
        (portadorAnillo ?p)
        (en ?p ?l)
        (lugarDestruccion ?l)
        (tieneObjeto ?p Anillo)
        (tieneObjeto ?p ChalecoMithril)
        (tieneObjeto ?p Espada)
      )
    :effect
      (anilloDestruido)
  )
)
