;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; dominio5.pddl
;; Ejercicio 5: Dominio con costes de viaje y extracción de recursos
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (domain dominio5)
  (:requirements 
    :typing         ;; Permite declarar jerarquía de tipos
    :adl            ;; Permite precondiciones avanzadas (forall, exists, etc.)
    :action-costs   ;; Permite usar funciones y actualizar costes
  )

  ;; ====================== 1. TIPOLOGÍA ============================
  (:types
    Personaje
    Enano Hobbit            - Personaje       ;; Subtipos de personaje
    Recurso
    Mineral Mithril Madera Especia Alimento   - Recurso   ;; Subtipos de recurso
    Localizacion
  )

  ;; ====================== 2. PREDICADOS ===========================
  (:predicates
    ;; “en ?x ?l” -> El objeto ?x (Personaje o Recurso) está en la Localizacion ?l
    (en ?x - (either Personaje Recurso) 
         ?l - Localizacion)

    ;; “camino ?l1 ?l2” -> Existe una arista (bidireccional) entre localidades ?l1 y ?l2
    (camino ?l1 - Localizacion 
            ?l2 - Localizacion)

    ;; “trabajando ?p ?l ?r” -> El Personaje ?p está extrayendo el Recurso ?r en la Localizacion ?l
    (trabajando ?p - Personaje 
                ?l - Localizacion 
                ?r - Recurso)

    ;; “disponible ?p” -> El Personaje ?p está libre para realizar una acción
    (disponible ?p - Personaje)
  )

  ;; ====================== 3. FUNCIONES =============================
  (:functions
    ;; “coste-camino ?l1 ?l2” -> Coste numérico asociado a viajar de ?l1 a ?l2
    (coste-camino ?l1 - Localizacion 
                  ?l2 - Localizacion) - number

    ;; “total-cost” -> Función acumuladora para sumar el coste total del plan
    (total-cost) - number
  )

  ;; ====================== 4. ACCIONES ==============================
  ;; Acción: Viajar
  ;;   Parámetros:
  ;;     ?p       - Personaje que se desplaza
  ;;     ?origen  - Localizacion de partida
  ;;     ?destino - Localizacion destino
  ;;
  ;; Semántica:
  ;;   - El personaje ?p debe estar en ?origen y libre (disponible).
  ;;   - Debe existir un “camino” de ?origen a ?destino.
  ;;   - Al ejecutar, ?p ya no está en ?origen, pasa a estar en ?destino.
  ;;   - Se incrementa (total-cost) en el valor de (coste-camino ?origen ?destino).
  (:action Viajar
    :parameters (?p      - Personaje
                 ?origen - Localizacion
                 ?destino - Localizacion)
    :precondition (and
      (en ?p ?origen)
      (camino ?origen ?destino)
      (disponible ?p)
    )
    :effect (and
      (not (en ?p ?origen))
      (en ?p ?destino)
      (increase (total-cost) (coste-camino ?origen ?destino))
    )
  )

  ;; Acción: ExtraerRecurso
  ;;   Parámetros:
  ;;     ?p - Enano (solo Enanos pueden extraer en este dominio)
  ;;     ?l - Localizacion donde se encuentra el recurso
  ;;     ?r - Recurso que se va a extraer
  ;;
  ;; Semántica:
  ;;   - El enano ?p debe estar en ?l y libre (disponible).
  ;;   - Se asume que “en ?r ?l” indica que el recurso está disponible en la ubicación.
  ;;   - Al ejecutar, ?p deja de estar disponible y pasa a “trabajando ?p ?l ?r”.
  ;;   - No hay incremento de coste aquí: se concentra en el coste de viajes, no de extracción.
  (:action ExtraerRecurso
    :parameters (?p - Enano
                 ?l - Localizacion
                 ?r - Recurso)
    :precondition (and
      (en ?p ?l)
      (disponible ?p)
      ;; Suponemos que “en ?r ?l” modela la existencia del nodo de recurso:
      (en ?r ?l)
    )
    :effect (and
      (not (disponible ?p))
      (trabajando ?p ?l ?r)
    )
  )
)
