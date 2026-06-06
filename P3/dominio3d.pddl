; Practica 3 - Ejercicio 3d
; Comunidad de 3 Hobbits, 1 Mago y 1 Elfo.
;
; Este dominio amplia 3c incorporando un Elfo a la Comunidad. La representacion
; sigue siendo compacta mediante comunidadEn, pero las acciones de formar y mover
; la Comunidad tienen mas parametros. Esto aumenta mucho el numero de instancias
; que debe generar el planificador y explica el salto de tiempo observado en este
; apartado.
;
; Se conserva hobbitAntes para romper simetrias entre los tres Hobbits; el Elfo
; no necesita orden auxiliar porque solo hay un personaje de tipo Elfo.

(define (domain tierra-media-ej3d)
  (:requirements :strips :typing :negative-preconditions :conditional-effects :equality)

  (:types
    Personaje Recurso Localizacion TipoPersonaje Objeto - object
  )

  (:constants
    Enano Hobbit Mago Elfo - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
    Anillo ChalecoMithril Espada - Objeto
  )

  (:predicates
    ; Predicados base: posiciones individuales para personajes no integrados en
    ; la Comunidad, recursos, mapa, tipos y estado de trabajo.
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

    ; Representacion de la Comunidad como grupo unico. comunidadEn guarda su
    ; posicion conjunta y evita modelar una posicion por cada miembro.
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)
    (comunidadEn ?l - Localizacion)
    ; Orden auxiliar para evitar permutaciones simetricas entre Hobbits. El
    ; problema define el orden canonico y las acciones lo exigen.
    (hobbitAntes ?h1 - Personaje ?h2 - Personaje)

    ; Objetos y predicados de progreso para la mision del Anillo.
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)
  )

  ; Desplazamiento individual solo para personajes disponibles que no sean
  ; miembros de la Comunidad. Los miembros viajan exclusivamente con
  ; viajarComunidad.
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

  ; Extraccion de recursos por personajes externos a la Comunidad. El personaje
  ; queda trabajando y deja de estar disponible para viajar o formar parte de
  ; otras acciones.
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and (disponible ?p) (not (miembroComunidad ?p)) (en ?p ?l) (recursoEn ?r ?l) (puedeExtraerPersonaje ?p ?r))
    :effect
      (and (not (disponible ?p)) (trabajando ?p ?l ?r) (recursoGenerado ?r) (recursoGeneradoEn ?r ?l))
  )

  ; Forma la Comunidad completa de 3 Hobbits, 1 Mago y 1 Elfo. La precondicion
  ; exige que todos esten disponibles y en la misma localizacion. hobbitAntes
  ; evita las permutaciones simetricas de los tres Hobbits.
  (:action formarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?hobbit3 - Personaje ?mago - Personaje ?elfo - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (comunidadFormada))
        (hobbitAntes ?hobbit1 ?hobbit2)
        (hobbitAntes ?hobbit2 ?hobbit3)
        (disponible ?hobbit1)
        (disponible ?hobbit2)
        (disponible ?hobbit3)
        (disponible ?mago)
        (disponible ?elfo)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
        (personajeEs ?hobbit3 Hobbit)
        (personajeEs ?mago Mago)
        (personajeEs ?elfo Elfo)
        (en ?hobbit1 ?l)
        (en ?hobbit2 ?l)
        (en ?hobbit3 ?l)
        (en ?mago ?l)
        (en ?elfo ?l)
      )
    :effect
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?hobbit3)
        (miembroComunidad ?mago)
        (miembroComunidad ?elfo)
        (comunidadEn ?l)
        (not (en ?hobbit1 ?l))
        (not (en ?hobbit2 ?l))
        (not (en ?hobbit3 ?l))
        (not (en ?mago ?l))
        (not (en ?elfo ?l))
      )
  )

  ; Mueve la Comunidad completa. En 3d esta accion tiene siete parametros
  ; incluyendo origen y destino, lo que incrementa de forma notable el numero de
  ; instancias respecto a 3a-3c.
  (:action viajarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?hobbit3 - Personaje ?mago - Personaje ?elfo - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (hobbitAntes ?hobbit1 ?hobbit2)
        (hobbitAntes ?hobbit2 ?hobbit3)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?hobbit3)
        (miembroComunidad ?mago)
        (miembroComunidad ?elfo)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
        (personajeEs ?hobbit3 Hobbit)
        (personajeEs ?mago Mago)
        (personajeEs ?elfo Elfo)
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

  ; El Mago miembro de la Comunidad materializa el Chaleco si el Mithril fue
  ; generado previamente en la localizacion actual del grupo.
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

  ; Un miembro de la Comunidad recoge objetos desde comunidadEn. Al recoger el
  ; Anillo, el mismo personaje queda marcado como portador y puede recoger los
  ; otros objetos obligatorios.
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

  ; Marca el objetivo como conseguido cuando el portador llega al lugar de
  ; destruccion con Anillo, ChalecoMithril y Espada.
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
