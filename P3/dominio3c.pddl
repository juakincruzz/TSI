; Practica 3 - Ejercicio 3c
; Comunidad de 3 Hobbits y 1 Mago.
;
; Este dominio amplia 3b con un tercer Hobbit. La decision central se mantiene:
; la Comunidad se representa con comunidadEn, por lo que el grupo tiene una sola
; posicion y no una posicion independiente para cada miembro.
;
; Con tres Hobbits habria muchas permutaciones equivalentes de los mismos
; miembros. hobbitAntes impone el orden Hobbit1 < Hobbit2 < Hobbit3 definido en
; el problema y evita que el planificador explore comunidades simetricas.

(define (domain tierra-media-ej3c)
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
    ; Predicados base heredados de ejercicios anteriores.
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

    ; Estado compacto de la Comunidad: una vez formada, comunidadEn indica la
    ; localizacion comun y los miembros ya no se desplazan individualmente.
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)
    (comunidadEn ?l - Localizacion)
    ; Orden auxiliar para evitar permutaciones simetricas entre Hobbits. En 3c
    ; se usa como cadena: hobbit1 antes que hobbit2 y hobbit2 antes que hobbit3.
    (hobbitAntes ?h1 - Personaje ?h2 - Personaje)

    ; Objetos, portador del Anillo y objetivo final.
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)
  )

  ; Accion de desplazamiento individual para personajes que no pertenecen a la
  ; Comunidad. La condicion (not (miembroComunidad ?p)) preserva la abstraccion
  ; de comunidadEn.
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

  ; Extrae un recurso con un personaje externo a la Comunidad y lo deja no
  ; disponible. recursoGeneradoEn permite exigir que el Mithril se genere en la
  ; misma localizacion donde se materializara el Chaleco.
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and (disponible ?p) (not (miembroComunidad ?p)) (en ?p ?l) (recursoEn ?r ?l) (puedeExtraerPersonaje ?p ?r))
    :effect
      (and (not (disponible ?p)) (trabajando ?p ?l ?r) (recursoGenerado ?r) (recursoGeneradoEn ?r ?l))
  )

  ; Forma la Comunidad de 3 Hobbits y 1 Mago. Las dos relaciones hobbitAntes
  ; fijan un orden canonico y descartan permutaciones equivalentes de Hobbits.
  (:action formarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?hobbit3 - Personaje ?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (not (comunidadFormada))
        (hobbitAntes ?hobbit1 ?hobbit2)
        (hobbitAntes ?hobbit2 ?hobbit3)
        (disponible ?hobbit1)
        (disponible ?hobbit2)
        (disponible ?hobbit3)
        (disponible ?mago)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
        (personajeEs ?hobbit3 Hobbit)
        (personajeEs ?mago Mago)
        (en ?hobbit1 ?l)
        (en ?hobbit2 ?l)
        (en ?hobbit3 ?l)
        (en ?mago ?l)
      )
    :effect
      (and
        (comunidadFormada)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?hobbit3)
        (miembroComunidad ?mago)
        (comunidadEn ?l)
        (not (en ?hobbit1 ?l))
        (not (en ?hobbit2 ?l))
        (not (en ?hobbit3 ?l))
        (not (en ?mago ?l))
      )
  )

  ; Mueve a todos los miembros como un grupo inseparable. Aunque los Hobbits se
  ; pasan como parametros, la posicion real del grupo es solo comunidadEn.
  (:action viajarComunidad
    :parameters (?hobbit1 - Personaje ?hobbit2 - Personaje ?hobbit3 - Personaje ?mago - Personaje ?origen - Localizacion ?destino - Localizacion)
    :precondition
      (and
        (comunidadFormada)
        (hobbitAntes ?hobbit1 ?hobbit2)
        (hobbitAntes ?hobbit2 ?hobbit3)
        (miembroComunidad ?hobbit1)
        (miembroComunidad ?hobbit2)
        (miembroComunidad ?hobbit3)
        (miembroComunidad ?mago)
        (personajeEs ?hobbit1 Hobbit)
        (personajeEs ?hobbit2 Hobbit)
        (personajeEs ?hobbit3 Hobbit)
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

  ; El Mago materializa el Chaleco en la posicion de la Comunidad cuando hay
  ; Mithril generado en esa misma localizacion.
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

  ; El portador del Anillo se decide al recogerlo. El efecto condicional habilita
  ; para ese portador la recogida del Chaleco y de la Espada.
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

  ; Finaliza la mision cuando el portador esta en el lugar de destruccion con
  ; todos los objetos necesarios.
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
