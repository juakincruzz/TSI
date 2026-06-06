; Practica 3 - Ejercicio 3a
; Comunidad de 1 Hobbit y 1 Mago.
;
; Este dominio extiende el Ejercicio 2 manteniendo la idea de representar la
; Comunidad como una unica entidad logica. En vez de desplazar por separado al
; Hobbit y al Mago, al formarse la Comunidad se elimina su posicion individual
; y se guarda una sola localizacion conjunta con comunidadEn.
;
; Esta decision reduce el numero de estados posibles: el planificador no tiene
; que considerar combinaciones de posiciones internas para los miembros de la
; Comunidad, sino solo la posicion global del grupo.

(define (domain tierra-media-ej3a)
  (:requirements :strips :typing :negative-preconditions :conditional-effects)

  (:types
    Personaje Recurso Localizacion TipoPersonaje Objeto - object
  )

  (:constants
    Enano Hobbit Mago Elfo - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
    Anillo ChalecoMithril Espada - Objeto
  )

  (:predicates
    ; Predicados heredados del dominio base: localizacion, recursos, caminos,
    ; tipos de personaje, disponibilidad y trabajo de extraccion.
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

    ; Estado de la Comunidad. comunidadEn sustituye a los predicados en(...)
    ; individuales de sus miembros una vez formada.
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)
    (comunidadEn ?l - Localizacion)

    ; Objetos necesarios para destruir el Anillo y control del orden de
    ; recogida: primero Anillo, despues Chaleco y Espada.
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)
  )

  ; Viajar individualmente solo esta permitido para personajes disponibles que
  ; no pertenezcan a la Comunidad. Los miembros de la Comunidad se mueven con
  ; viajarComunidad para mantener una unica posicion conjunta.
  (:action Viajar
    :parameters (?p - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and (disponible ?p) (not (miembroComunidad ?p)) (en ?p ?origen) (camino ?origen ?destino))
    :effect
      (and
        (not (en ?p ?origen))
        (en ?p ?destino)
        (when (caminoDestructible ?origen ?destino)
          (and (not (camino ?origen ?destino)) (not (camino ?destino ?origen)))
        )
      )
  )

  ; Un personaje disponible y fuera de la Comunidad queda trabajando en el nodo
  ; del recurso. Se genera tanto el recurso global como el par recurso/lugar,
  ; necesario para materializar el Chaleco en la misma localizacion.
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and (disponible ?p) (not (miembroComunidad ?p)) (en ?p ?l) (recursoEn ?r ?l) (puedeExtraerPersonaje ?p ?r))
    :effect
      (and (not (disponible ?p)) (trabajando ?p ?l ?r) (recursoGenerado ?r) (recursoGeneradoEn ?r ?l))
  )

  ; Forma la Comunidad reducida exigida en 3a: exactamente un Hobbit y un Mago
  ; que esten disponibles y en la misma localizacion. Tras formarla, se retiran
  ; sus posiciones individuales y se registra comunidadEn(?l).
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
        (comunidadEn ?l)
        (not (en ?hobbit ?l))
        (not (en ?mago ?l))
      )
  )

  ; Mueve la Comunidad como bloque. La accion conserva el comportamiento de los
  ; caminos destructibles: si el grupo cruza Rivendell-Moria, el arco desaparece
  ; en ambos sentidos igual que en Viajar.
  (:action viajarComunidad
    :parameters (?hobbit - Personaje ?mago - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit)
        (miembroComunidad ?mago)
        (personajeEs ?hobbit Hobbit)
        (personajeEs ?mago Mago)
        (comunidadEn ?origen)
        (camino ?origen ?destino)
      )
    :effect
      (and
        (not (comunidadEn ?origen))
        (comunidadEn ?destino)
        (when (caminoDestructible ?origen ?destino)
          (and (not (camino ?origen ?destino)) (not (camino ?destino ?origen)))
        )
      )
  )

  ; El Mago de la Comunidad puede materializar el Chaleco solo si la Comunidad
  ; esta en una localizacion donde ya se ha generado Mithril.
  (:action materializarChaleco
    :parameters (?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (chalecoMaterializado))
        (comunidadFormada)
        (miembroComunidad ?mago)
        (personajeEs ?mago Mago)
        (comunidadEn ?l)
        (recursoGeneradoEn Mithril ?l)
      )
    :effect
      (and (chalecoMaterializado) (objetoEn ChalecoMithril ?l))
  )

  ; Permite que un miembro de la Comunidad recoja objetos. Recoger el Anillo
  ; marca al portador y habilita dinamicamente que ese mismo personaje pueda
  ; recoger el ChalecoMithril y la Espada.
  (:action recogerObjeto
    :parameters (?p - Personaje ?l - Localizacion ?o - Objeto)
    :precondition
      (and (comunidadFormada) (miembroComunidad ?p) (comunidadEn ?l) (objetoEn ?o ?l) (puedeRecogerObjeto ?p ?o))
    :effect
      (and
        (not (objetoEn ?o ?l))
        (tieneObjeto ?p ?o)
        (when (objetoEsAnillo ?o)
          (and (portadorAnillo ?p) (puedeRecogerObjeto ?p ChalecoMithril) (puedeRecogerObjeto ?p Espada))
        )
      )
  )

  ; Objetivo final del dominio: el portador debe estar con la Comunidad en el
  ; lugar de destruccion y tener los tres objetos requeridos.
  (:action destruirAnillo
    :parameters (?p - Personaje ?l - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?p)
        (portadorAnillo ?p)
        (comunidadEn ?l)
        (lugarDestruccion ?l)
        (tieneObjeto ?p Anillo)
        (tieneObjeto ?p ChalecoMithril)
        (tieneObjeto ?p Espada)
      )
    :effect
      (anilloDestruido)
  )
)
