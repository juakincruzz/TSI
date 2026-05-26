; Practica 3 - Ejercicio 4
; Crear Uruk-Hai en Torre de Hechiceria en Isengard.
; Se parte del Ejercicio 3a (Comunidad reducida de 1 Hobbit y 1 Mago)
; y se extiende con:
;   - Nuevos tipos de personaje: Orco, Humano, Corsario.
;   - Tipos de edificio: TorreHechiceria y Extractor.
;   - Restriccion "maligna": ni el Extractor ni la Torre pueden construirse
;     con recursos extraidos por Enanos. Esto se impone en Construir con
;     forall + exists (como exige el enunciado), sin restringir la extraccion
;     general que los Enanos puedan hacer fuera de ese contexto.
;   - Para que un Orco extraiga Mineral, debe existir un Extractor en su
;     localizacion. Esto NO afecta a los Enanos.
;   - Para crear Uruk-Hai, la Especia debe transportarse fisicamente desde
;     Tolfolas hasta Isengard (otro Corsario la recoge y viaja con ella).

(define (domain tierra-media-ej4)
  (:requirements :adl)

  (:types
    Personaje Recurso Localizacion TipoPersonaje Objeto TipoEdificio - object
  )

  (:constants
    Enano Hobbit Mago Elfo Orco Humano Corsario - TipoPersonaje
    Mineral Mithril Madera Especia Alimento - Recurso
    Anillo ChalecoMithril Espada - Objeto
    TorreHechiceria Extractor - TipoEdificio
  )

  (:predicates
    ; --- Heredados del Ejercicio 3a ---
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
    (comunidadFormada)
    (miembroComunidad ?p - Personaje)
    (comunidadEn ?l - Localizacion)
    (objetoEn ?o - Objeto ?l - Localizacion)
    (tieneObjeto ?p - Personaje ?o - Objeto)
    (objetoEsAnillo ?o - Objeto)
    (puedeRecogerObjeto ?p - Personaje ?o - Objeto)
    (portadorAnillo ?p - Personaje)
    (chalecoMaterializado)
    (lugarDestruccion ?l - Localizacion)
    (anilloDestruido)

    ; --- Nuevos predicados del Ejercicio 4 ---

    ; Edificio (Torre o Extractor) construido en una localizacion.
    (edificioEn ?te - TipoEdificio ?l - Localizacion)

    ; Que recurso necesita cada tipo de edificio para ser construido.
    ; Se declara en el problema. Construir lo recorre con forall.
    (necesita ?te - TipoEdificio ?r - Recurso)

    ; Un Corsario porta Especia tras recogerla.
    (transportandoEspecia ?p - Personaje)

    ; Senalizador de que se ha creado un Uruk-Hai.
    (urukHaiCreado)
  )

  ; ---------------------------------------------------------------------------
  ; Viajar
  ; ---------------------------------------------------------------------------
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
        ; Camino destructible (Rivendell-Moria) se destruye al transitarlo.
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
  ; Se anade una precondicion implicativa: si el personaje es un Orco y el
  ; recurso es Mineral, entonces debe existir un Extractor en la localizacion.
  ; Esto NO afecta a los Enanos: un Enano puede seguir extrayendo Mineral en
  ; cualquier localizacion con nodo de Mineral, sin Extractor.
  (:action ExtraerRecurso
    :parameters (?p - Personaje ?l - Localizacion ?r - Recurso)
    :precondition
      (and
        (disponible ?p)
        (not (miembroComunidad ?p))
        (en ?p ?l)
        (recursoEn ?r ?l)
        (puedeExtraerPersonaje ?p ?r)
        ; Restriccion: Orco + Mineral requiere Extractor en la localizacion.
        (imply
          (and (personajeEs ?p Orco) (= ?r Mineral))
          (edificioEn Extractor ?l)
        )
      )
    :effect
      (and
        (not (disponible ?p))
        (trabajando ?p ?l ?r)
        (recursoGenerado ?r)
        (recursoGeneradoEn ?r ?l)
      )
  )

  ; ---------------------------------------------------------------------------
  ; formarComunidad (3a: 1 Hobbit + 1 Mago)
  ; ---------------------------------------------------------------------------
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

  ; ---------------------------------------------------------------------------
  ; viajarComunidad (3a: 1 Hobbit + 1 Mago)
  ; ---------------------------------------------------------------------------
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

  ; ---------------------------------------------------------------------------
  ; materializarChaleco (heredada del Ej. 2/3a)
  ; ---------------------------------------------------------------------------
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

  ; ---------------------------------------------------------------------------
  ; recogerObjeto (heredada del Ej. 2/3a)
  ; ---------------------------------------------------------------------------
  (:action recogerObjeto
    :parameters (?p - Personaje ?l - Localizacion ?o - Objeto)
    :precondition
      (and
        (comunidadFormada)
        (miembroComunidad ?p)
        (comunidadEn ?l)
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

  ; ---------------------------------------------------------------------------
  ; destruirAnillo (heredada del Ej. 2/3a)
  ; ---------------------------------------------------------------------------
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

  ; ---------------------------------------------------------------------------
  ; Construir (Ejercicio 4)
  ; ---------------------------------------------------------------------------
  ; Parametros: personaje, tipo de edificio, localizacion.
  ;
  ; Logica:
  ;   - El personaje debe estar disponible en la localizacion y no pertenecer
  ;     a la Comunidad (los magos de la Comunidad no construyen la Torre).
  ;   - No puede haber ya un edificio de ese tipo en la misma ubicacion.
  ;   - Mago construye TorreHechiceria; Humano construye Extractor.
  ;   - Para cada recurso ?r que el edificio necesita (necesita ?te ?r),
  ;     debe existir algun personaje no-Enano trabajando ese recurso en
  ;     alguna localizacion. Este es el uso obligatorio de forall + exists
  ;     y modela el caracter "maligno" de la construccion.
  ;   - Tras construir, el personaje vuelve a estar disponible (nota del
  ;     enunciado). En este modelo Construir no exige (not (disponible)),
  ;     por lo que solo aseguramos (disponible) en el efecto por claridad.
  (:action Construir
    :parameters (?p - Personaje ?te - TipoEdificio ?l - Localizacion)
    :precondition
      (and
        (disponible ?p)
        (not (miembroComunidad ?p))
        (en ?p ?l)
        (not (edificioEn ?te ?l))
        (or
          (and (personajeEs ?p Mago) (= ?te TorreHechiceria))
          (and (personajeEs ?p Humano) (= ?te Extractor))
        )
        (forall (?r - Recurso)
          (imply (necesita ?te ?r)
            (exists (?p2 - Personaje ?l2 - Localizacion)
              (and
                (trabajando ?p2 ?l2 ?r)
                (not (personajeEs ?p2 Enano))
              )
            )
          )
        )
      )
    :effect
      (and
        (edificioEn ?te ?l)
        ; Excepcion del enunciado: el constructor vuelve a estar disponible.
        (disponible ?p)
      )
  )

  ; ---------------------------------------------------------------------------
  ; RecogerEspecia (Ejercicio 4)
  ; ---------------------------------------------------------------------------
  (:action RecogerEspecia
    :parameters (?p - Personaje ?l - Localizacion)
    :precondition
      (and
        (disponible ?p)
        (personajeEs ?p Corsario)
        (en ?p ?l)
        (recursoGeneradoEn Especia ?l)
        (not (transportandoEspecia ?p))
      )
    :effect
      (transportandoEspecia ?p)
  )

  ; ---------------------------------------------------------------------------
  ; CrearUrukHai (Ejercicio 4)
  ; ---------------------------------------------------------------------------
  (:action CrearUrukHai
    :parameters (?mago - Personaje ?l - Localizacion)
    :precondition
      (and
        (disponible ?mago)
        (not (miembroComunidad ?mago))
        (personajeEs ?mago Mago)
        (en ?mago ?l)
        (edificioEn TorreHechiceria ?l)
        (exists (?c - Personaje)
          (and (en ?c ?l) (transportandoEspecia ?c))
        )
      )
    :effect
      (urukHaiCreado)
  )
)
