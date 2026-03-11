;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dominio4.pddl
;; Ejercicio 4: construir Fortaleza en Isengard
;; Dominio con tipologías de personajes, recursos y edificios,
;; y acciones para extraer recursos y construir.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain dominio4)
  (:requirements 
    :strips 
    :typing 
    :adl 
    :negative-preconditions
  )

  ;; ====================== 1. TIPOLOGÍA ==============================
  (:types 
    personaje
      hobbit    humano    enano    mago    elfo  - personaje

    localizacion

    recurso

    edificio
      extractor    fortaleza  - edificio
  )

  ;; ====================== 2. CONSTANTES ============================
  ;; Definimos directamente los nombres de recurso aquí para
  ;; poder referirnos a ellos en precondiciones (igualdad).
  (:constants 
    Alimento   Mineral   Madera   Mithril  - recurso
  )

  ;; ====================== 3. PREDICADOS ============================
  ;; “en ?p ?loc”                   -> Personaje o Edificio está en Localización
  ;; “disponible ?p”                -> Personaje está libre para actuar
  ;; “camino ?l1 ?l2”               -> Existe arco bidireccional entre dos Localizaciones
  ;; “trabajando ?p ?loc ?r”        -> Personaje está extrayendo un Recurso en Localización
  ;; “recurso-disponible ?loc ?r”   -> En esa Localización existe nodo del Recurso
  ;; “requiere-recurso ?e ?r”       -> Edificio necesita (al menos) Recurso para construirse
  ;; “es-<tipo>”                    -> Clasificación de Personajes y Edificios
  ;; “edificio-construido ?e ?loc”  -> Edificio ha sido construido en Localización
  (:predicates 
    (en                     ?p - personaje     ?loc - localizacion)
    (disponible             ?p - personaje)
    (camino                 ?l1 - localizacion ?l2 - localizacion)
    (trabajando             ?p - personaje     ?loc - localizacion ?r - recurso)
    (recurso-disponible     ?loc - localizacion ?r - recurso)
    (requiere-recurso       ?e - edificio      ?r - recurso)

    (es-hobbit               ?p - personaje)
    (es-humano               ?p - personaje)
    (es-enano                ?p - personaje)
    (es-mago                 ?p - personaje)
    (es-elfo                 ?p - personaje)

    (es-extractor            ?e - edificio)
    (es-fortaleza            ?e - edificio)

    (edificio-construido     ?e - edificio      ?loc - localizacion)
  )

  ;; ====================== 4. ACCIONES ==============================
  ;; Acción: Viajar
  ;; Parámetros: ?p (personaje), ?from (localización de origen), ?to (localización destino)
  ;; Precondición: 
  ;;   - ?p está en “from”
  ;;   - ?p está disponible
  ;;   - Existe un arco (camino) entre from y to
  ;; Efecto:
  ;;   - ?p ya no está en “from”
  ;;   - ?p queda en “to”
  (:action Viajar
    :parameters (?p   - personaje 
                 ?from - localizacion 
                 ?to   - localizacion)
    :precondition (and 
      (en ?p ?from)
      (disponible ?p)
      (camino ?from ?to)
    )
    :effect (and 
      (not (en ?p ?from))
      (en ?p ?to)
    )
  )

  ;; Acción: ExtraerRecurso
  ;; Parámetros: ?p   (personaje), ?loc (localización), ?res (recurso)
  ;; Precondición:
  ;;   - ?p está en “loc” y está disponible
  ;;   - En “loc” existe nodo del recurso ?res
  ;;   - Se cumple una de las dos situaciones:
  ;;     1) ?res = Alimento  &&  ?p es hobbit
  ;;     2) ?res pertenece {Mineral, Madera, Mithril}  &&  todos los hobbits 
  ;;        están trabajando Alimento en alguna localización  &&  
  ;;        se cumple la condición específica:
  ;;          a) Si ?res = Mineral -> ?p es humano
  ;;          b) Si ?res = Madera  -> ?p es enano
  ;;          c) Si ?res = Mithril -> ?p es enano  &&  
  ;;             existe extractor construido en “loc”
  ;; Efecto:
  ;;   - ?p deja de estar disponible
  ;;   - ?p pasa a “trabajando” ?res en ?loc
  (:action ExtraerRecurso
    :parameters (?p   - personaje 
                 ?loc - localizacion 
                 ?res - recurso)
    :precondition (and 
      (en ?p ?loc)
      (disponible ?p)
      (recurso-disponible ?loc ?res)
      (or 
        ;; Caso A: Extraer Alimento (solo hobbits)
        (and 
          (= ?res Alimento) 
          (es-hobbit ?p)
        )
        ;; Caso B: Extraer Mineral, Madera o Mithril
        (and 
          (or 
            (= ?res Mineral) 
            (= ?res Madera) 
            (= ?res Mithril)
          )
          ;; Todos los hobbits están trabajando Alimento (en cualquier localización)
          (forall (?h - hobbit) 
            (exists (?lh - localizacion) 
              (trabajando ?h ?lh Alimento)
            )
          )
          ;; Condición específica según el recurso
          (or 
            ;; Mineral -> ?p es humano
            (and 
              (= ?res Mineral) 
              (es-humano ?p)
            )
            ;; Madera -> ?p es enano
            (and 
              (= ?res Madera) 
              (es-enano ?p)
            )
            ;; Mithril -> ?p es enano  &&  existe extractor en “loc”
            (and 
              (= ?res Mithril) 
              (es-enano ?p) 
              (exists (?ex - extractor) 
                (and 
                  (es-extractor ?ex) 
                  (edificio-construido ?ex ?loc)
                )
              )
            )
          )
        )
      )
    )
    :effect (and 
      (not (disponible ?p))
      (trabajando ?p ?loc ?res)
    )
  )

  ;; -------------------------------------------------------------------
  ;; Acción: Construir
  ;; Parámetros: ?p   (personaje), ?e (edificio), ?loc (localización)
  ;; Precondición:
  ;;   - ?p está en “loc” y está disponible
  ;;   - ?e NO está ya construido en “loc”
  ;;   - Para construir:
  ;;       – Si ?e es fortaleza -> ?p debe ser mago
  ;;       – Si ?e es extractor  -> ?p debe ser humano
  ;;   - Para cada recurso requerido por ?e, debe existir algún personaje
  ;;     trabajando ese recurso (en cualquier localización)
  ;; Efecto:
  ;;   - Se marca “edificio-construido ?e ?loc”
  ;;   - ?p recupera disponibilidad
  (:action Construir
    :parameters (?p   - personaje 
                 ?e   - edificio 
                 ?loc - localizacion)
    :precondition (and 
      (en ?p ?loc)
      (disponible ?p)
      (not (edificio-construido ?e ?loc))
      (or 
        (and (es-fortaleza ?e) (es-mago ?p))
        (and (es-extractor ?e) (es-humano ?p))
      )
      (forall (?r_necesario - recurso)
        (imply 
          (requiere-recurso ?e ?r_necesario)
          (exists (?cualquier_trabajador - personaje 
                   ?cualquier_lugar      - localizacion)
            (trabajando ?cualquier_trabajador 
                        ?cualquier_lugar 
                        ?r_necesario)
          )
        )
      )
    )
    :effect (and 
      (edificio-construido ?e ?loc)
      (disponible ?p)
    )
  )
)
