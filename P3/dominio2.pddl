;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dominio2.pddl
;; Ejercicio 2: “Formar Comunidad reducida y destruir el Anillo”
;;
;; Partimos de dominio1 con las siguientes ampliaciones:
;;  1) Nuevos tipos de personaje (Mago).
;;  2) Predicados para modelar la Comunidad (Formada, Miembros, Lugar).
;;  3) Acciones específicas:
;;       - ViajarIndividual (heredado de Viajar pero bloqueado si estás en la Comunidad).
;;       - formarComunidad (un Hobbit + un Mago en la misma Localización).
;;       - viajarComunidad (mover los dos miembros juntos).
;;       - recogerAnillo, recogerEspada, recogerMithril (solo el portador puede recoger).
;;       - destruirAnillo (solo cuando se hayan recogido Anillo+Espada+Mithril y estés en Orodruin).
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain dominio2)
  ;; Necesitamos :typing para jerarquía de tipos y :adl para usar “imply” y “when” si hiciera falta.
  (:requirements :typing :adl)

  ;; ===================================================================
  ;; 1. TIPOLOGÍA (:types)
  ;;
  ;;   - Personaje  : Enano, Hobbit, Mago
  ;;   - Recurso    : Anillo, Espada, Mithril, (y heredados del ejercicio 1: Madera, Mineral, Especia, Alimento)
  ;;   - Localizacion: idéntico a dominio1
  ;; ===================================================================
  (:types
    Enano Hobbit Mago - Personaje
    Anillo Espada Mithril Madera Mineral Especia Alimento - Recurso
    Localizacion
  )

  ;; ===================================================================
  ;; 2. PREDICADOS (:predicates)
  ;;
  ;;  - (en ?x ?l)            -> heredado: posición de Personaje o Recurso en una Localizacion.
  ;;  - (camino ?l1 ?l2)      -> heredado: arista bidireccional entre dos Localizaciones.
  ;;  - (trabajando ?p ?l ?r) -> heredado: un Personaje extrae un Recurso en una Localizacion.
  ;;  - (disponible ?p)       -> heredado: Personaje que NO esté trabajando.
  ;;
  ;;  - (comunidadFormada)       -> true si ya se invocó formarComunidad.
  ;;  - (enComunidad ?p)         -> true si el personaje pertenece a la Comunidad.
  ;;  - (lugarComunidad ?l)      -> localización actual de toda la Comunidad.
  ;;  - (anilloRecogido ?p)      -> true si el Hobbit ?p se ha hecho con el Anillo.
  ;;  - (espadaRecogida ?p)      -> true si el Hobbit ?p ya guardó la Espada.
  ;;  - (mithrilRecogido ?p)     -> true si el Hobbit ?p ya guardó el Mithril.
  ;;  - (anilloDestruido)        -> true si se invocó destruirAnillo.
  ;;  - (lugarDestruccion ?l)    -> sitio (Orodruin) donde se debe destruir el Anillo.
  ;; ===================================================================
  (:predicates
    ;; heredados de dominio1
    (en            ?x - (either Personaje Recurso) ?l - Localizacion)
    (camino        ?l1 - Localizacion               ?l2 - Localizacion)
    (trabajando    ?p - Personaje ?l - Localizacion ?r - Recurso)
    (disponible    ?p - Personaje)

    ;; nuevos para Ejercicio 2
    (comunidadFormada)                        ;; sin parámetros
    (enComunidad     ?p - Personaje)          ;; marca miembro de la Comunidad
    (lugarComunidad  ?l - Localizacion)   ;; localización actual de la Comunidad
    (anilloRecogido  ?p - Hobbit)             ;; únicamente Hobbits pueden recoger Anillo
    (espadaRecogida  ?p - Hobbit)            ;; y la Espada, y el Mithril
    (mithrilRecogido ?p - Hobbit)
    (anilloDestruido)                        ;; objetivo final
    (lugarDestruccion ?l - Localizacion)     ;; sitio (Orodruin) donde se destruye el Anillo
  )

  ;; ===================================================================
  ;; 3. ACCIONES 
  ;; ===================================================================

  ;; ===================================================================
  ;; 3.1. ViajarIndividual
  ;;
  ;;  - Un Personaje solo puede moverse individualmente si:
  ;;      * la Comunidad NO está formada,  OR
  ;;      * está formada pero el personaje NO es miembro (enComunidad ?p = falso).
  ;;
  ;;  - Parámetros: ?p - Personaje, ?origen, ?destino - Localizacion
  ;;  - Precondiciones:
  ;;      (en ?p ?origen)
  ;;      (camino ?origen ?destino)
  ;;      (disponible ?p)
  ;;      (or (not (comunidadFormada)) (not (enComunidad ?p)))
  ;;  - Efectos:
  ;;      (not (en ?p ?origen))
  ;;      (en   ?p ?destino)
  ;; ===================================================================
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

  ;; ===================================================================
  ;; 3.2. formarComunidad
  ;;
  ;;  - Parámetros: ?p1 - Personaje, ?p2 - Personaje, ?l - Localizacion
  ;;  - Precondiciones:
  ;;      (disponible ?p1)
  ;;      (disponible ?p2)
  ;;      (en ?p1 ?l)         ; ambos en la misma localización
  ;;      (en ?p2 ?l)
  ;;      (or
  ;;        (and (exists (?d - Hobbit) (= ?d ?p1))
  ;;             (exists (?d2 - Mago)  (= ?d2 ?p2)))
  ;;        (and (exists (?d - Mago)  (= ?d ?p1))
  ;;             (exists (?d2 - Hobbit)(= ?d2 ?p2)))
  ;;      )
  ;;      (not (comunidadFormada))
  ;;  - Efectos:
  ;;      (not (disponible ?p1))  ; dejan de estar disponibles
  ;;      (not (disponible ?p2))
  ;;      (enComunidad ?p1)
  ;;      (enComunidad ?p2)
  ;;      (comunidadFormada)
  ;;      (lugarComunidad ?l)     ; la Comunidad queda “en” ?l
  ;;
  ;;  
  ;; ===================================================================
  (:action formarComunidad
    :parameters (?p1 - Personaje
                 ?p2 - Personaje
                 ?l  - Localizacion)
    :precondition (and
      (disponible ?p1)
      (disponible ?p2)
      (en ?p1 ?l)
      (en ?p2 ?l)
      (or
        (and
          (exists (?d - Hobbit)   (and (= ?d ?p1)))
          (exists (?d2 - Mago)    (and (= ?d2 ?p2))))
        (and
          (exists (?d - Mago)     (and (= ?d ?p1)))
          (exists (?d2 - Hobbit)  (and (= ?d2 ?p2))))
      )
      (not (comunidadFormada))
    )
    :effect (and
      (not (disponible ?p1))
      (not (disponible ?p2))
      (enComunidad ?p1)
      (enComunidad ?p2)
      (comunidadFormada)
      (lugarComunidad ?l)
    )
  )

  ;; ===================================================================
  ;; 3.3. viajarComunidad
  ;;
  ;;  - Parámetros:
  ;;      ?origen  - Localizacion
  ;;      ?destino - Localizacion
  ;;      ?p1 - Personaje  (miembro 1)
  ;;      ?p2 - Personaje  (miembro 2)
  ;;  - Precondiciones:
  ;;      (comunidadFormada)
  ;;      (lugarComunidad ?origen)
  ;;      (en ?p1 ?origen), (en ?p2 ?origen)
  ;;      (enComunidad ?p1), (enComunidad ?p2)
  ;;      (camino ?origen ?destino)
  ;;  - Efectos:
  ;;      (not (en ?p1 ?origen)) (en ?p1 ?destino)
  ;;      (not (en ?p2 ?origen)) (en ?p2 ?destino)
  ;;      (not (lugarComunidad ?origen)) (lugarComunidad ?destino)
  ;; ===================================================================
  (:action viajarComunidad
    :parameters (?origen - Localizacion
                 ?destino - Localizacion
                 ?p1 - Personaje
                 ?p2 - Personaje)
    :precondition (and
      (comunidadFormada)
      (lugarComunidad ?origen)
      (en ?p1 ?origen)
      (en ?p2 ?origen)
      (enComunidad ?p1)
      (enComunidad ?p2)
      (camino ?origen ?destino)
    )
    :effect (and
      (not (en ?p1 ?origen)) (en ?p1 ?destino)
      (not (en ?p2 ?origen)) (en ?p2 ?destino)
      (not (lugarComunidad ?origen)) (lugarComunidad ?destino)
    )
  )

  ;; ===================================================================
  ;; 3.4. recogerAnillo
  ;;
  ;;  - Parámetros: ?p - Hobbit, ?l - Localizacion, ?r - Anillo
  ;;  - Precondiciones:
  ;;      (comunidadFormada)
  ;;      (en ?p ?l)
  ;;      (enComunidad ?p)
  ;;      (en ?r ?l)            ; el Anillo1 está en esa localización
  ;;      (not (anilloRecogido ?p))
  ;;  - Efectos:
  ;;      (anilloRecogido ?p)
  ;; ===================================================================
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

  ;; ===================================================================
  ;; 3.5. recogerEspada
  ;;
  ;;  - Parámetros: ?p - Hobbit, ?l - Localizacion, ?r - Espada
  ;;  - Precondiciones:
  ;;      (comunidadFormada)
  ;;      (en ?p ?l)
  ;;      (enComunidad ?p)
  ;;      (anilloRecogido ?p)   ; sólo el portador del Anillo (Hobbit) puede hacerlo
  ;;      (en ?r ?l)            ; la Espada está en esa localización
  ;;      (not (espadaRecogida ?p))
  ;;  - Efectos:
  ;;      (espadaRecogida ?p)
  ;;      
  ;; ===================================================================
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

  ;; ===================================================================
  ;; 3.6. recogerMithril
  ;;
  ;;  - Parámetros: ?p - Hobbit, ?l - Localizacion, ?r - Mithril
  ;;  - Precondiciones:
  ;;      (comunidadFormada)
  ;;      (en ?p ?l)
  ;;      (enComunidad ?p)
  ;;      (anilloRecogido ?p)
  ;;      (en ?r ?l)            ; Mithril está en esa localización
  ;;      (not (mithrilRecogido ?p))
  ;;  - Efectos:
  ;;      (mithrilRecogido ?p)
  ;;      
  ;; ===================================================================
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

  ;; ===================================================================
  ;; 3.7. destruirAnillo
  ;;
  ;;  - Parámetros: ?p - Hobbit, ?l - Localizacion
  ;;  - Precondiciones:
  ;;      (anilloRecogido ?p)
  ;;      (espadaRecogida ?p)
  ;;      (mithrilRecogido ?p)
  ;;      (lugarDestruccion ?l)  ; Orodruin
  ;;      (en ?p ?l)
  ;;  - Efectos:
  ;;      (anilloDestruido)
  ;; ===================================================================
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
