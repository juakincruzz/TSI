; Practica 3 - Ejercicio 3b
; Comunidad de 2 Hobbits y 1 Mago.
;
; Este dominio amplia 3a incorporando un segundo Hobbit a la Comunidad. La
; representacion sigue usando comunidadEn para mantener una unica localizacion
; del grupo y evitar que Fast Downward tenga que razonar sobre posiciones
; individuales de cada miembro.
;
; Al haber dos Hobbits aparecen soluciones simetricas: intercambiar Hobbit1 y
; Hobbit2 en los parametros de las acciones produce planes equivalentes. Para
; reducir esa exploracion redundante se introduce hobbitAntes como orden canonico.

(define (domain tierra-media-ej3b)
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
    ; Predicados heredados: localizacion individual, recursos, mapa, tipos,
    ; disponibilidad y generacion de recursos.
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

    ; Comunidad representada como una unica entidad: al formarse, sus miembros
    ; dejan de tener posicion individual y pasan a compartir comunidadEn.
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)
    (comunidadEn ?l - Localizacion)
    ; Orden auxiliar para evitar permutaciones simetricas entre Hobbits. El
    ; problema inicializa solo el orden aceptado, por ejemplo Hobbit1 antes que
    ; Hobbit2, y las acciones lo exigen en sus precondiciones.
    (hobbitAntes ?h1 - Personaje ?h2 - Personaje)

    ; Objetos de la mision y control del portador del Anillo.
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)
  )

  ; Viajar individualmente queda prohibido para miembros de la Comunidad. Esto
  ; evita estados incoherentes donde un miembro se separa del grupo despues de
  ; haber sido incorporado.
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

  ; Solo personajes disponibles y fuera de la Comunidad pueden quedar trabajando
  ; en un recurso. El recursoGeneradoEn guarda la localizacion donde se produjo.
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and (disponible ?p) (not (miembroComunidad ?p)) (en ?p ?l) (recursoEn ?r ?l) (puedeExtraerPersonaje ?p ?r))
    :effect
      (and (not (disponible ?p)) (trabajando ?p ?l ?r) (recursoGenerado ?r) (recursoGeneradoEn ?r ?l))
  )

  ; Forma la Comunidad de 2 Hobbits y 1 Mago. hobbitAntes fija un unico orden
  ; para los Hobbits y elimina la accion simetrica con los Hobbits intercambiados.
  (:action formarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (comunidadFormada))
        (hobbitAntes ?hobbit1 ?hobbit2)
        (disponible ?hobbit1)
        (disponible ?hobbit2)
        (disponible ?mago)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
        (personajeEs ?mago Mago)
        (en ?hobbit1 ?l)
        (en ?hobbit2 ?l)
        (en ?mago ?l)
      )
    :effect
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?mago)
        (comunidadEn ?l)
        (not (en ?hobbit1 ?l))
        (not (en ?hobbit2 ?l))
        (not (en ?mago ?l))
      )
  )

  ; Mueve la Comunidad completa como una sola entidad. Se repite hobbitAntes en
  ; la precondicion para que tambien las instancias de movimiento respeten el
  ; mismo orden canonico entre Hobbits.
  (:action viajarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?mago - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (hobbitAntes ?hobbit1 ?hobbit2)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?mago)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
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

  ; El Chaleco se materializa en la posicion conjunta de la Comunidad si alli
  ; existe Mithril generado previamente.
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

  ; Recoge objetos desde la localizacion conjunta de la Comunidad. Al recoger el
  ; Anillo se habilita al portador para recoger los objetos restantes.
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

  ; Destruye el Anillo cuando el portador, integrado en la Comunidad, esta en
  ; Orodruin y posee Anillo, ChalecoMithril y Espada.
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
