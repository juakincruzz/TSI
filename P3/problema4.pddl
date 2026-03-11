;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; problema4.pddl
;; Ejercicio 4: Construir Fortaleza en Isengard
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (problem problema4)
  (:domain dominio4)

  ;; =================== 1. OBJETOS ================================
  (:objects
    ;; Personajes
    Hobbit1 Hobbit2 Hobbit3 Hobbit4    - hobbit
    Humano1 Humano2                    - humano
    Enano1   Enano2                    - enano
    Mago1    Mago2                     - mago
    Elfo1                              - elfo

    ;; Edificios
    Extractor1                         - extractor
    Fortaleza1                         - fortaleza

    ;; Localizaciones (CamelCase, como en los caminos)
    Hobbiton Bree Tharbad Rivendell Edoras Moria Lothlorien Erebor Fangorn Isengard Mirkwood
    HelmsDeep HighPass AmonHen DeadMarshes MinasMorgul Orodruin MinasTirith Tolfolas DolAmroth  - localizacion
  )

  ;; =================== 2. ESTADO INICIAL (:init) =================
  (:init
    ;; 2.1. Tipos de personaje
    (es-hobbit Hobbit1) (es-hobbit Hobbit2) (es-hobbit Hobbit3) (es-hobbit Hobbit4)
    (es-humano Humano1) (es-humano Humano2)
    (es-enano  Enano1)   (es-enano  Enano2)
    (es-mago   Mago1)    (es-mago   Mago2)
    (es-elfo   Elfo1)

    ;; 2.2. Tipos de edificio
    (es-extractor  Extractor1)
    (es-fortaleza  Fortaleza1)

    ;; 2.3. Ubicaciones iniciales de personajes
    (en Hobbit1 Hobbiton) (en Hobbit2 Hobbiton) (en Hobbit3 Hobbiton) (en Hobbit4 Bree)
    (en Mago1 Rivendell)  (en Mago2 Isengard)
    (en Enano1 Fangorn)   (en Enano2 Erebor)
    (en Humano1 Edoras)   (en Humano2 Edoras)
    (en Elfo1 Lothlorien)

    ;; 2.4. Disponibilidad inicial
    (disponible Hobbit1) (disponible Hobbit2) (disponible Hobbit3) (disponible Hobbit4)
    (disponible Humano1) (disponible Humano2)
    (disponible Enano1)   (disponible Enano2)
    (disponible Mago1)    (disponible Mago2)
    (disponible Elfo1)

    ;; 2.5. Nodos de recurso (recurso-disponible)
    (recurso-disponible Fangorn      Madera)
    (recurso-disponible Lothlorien   Madera)
    (recurso-disponible Mirkwood     Madera)
    (recurso-disponible Moria        Mineral)
    (recurso-disponible Erebor       Mineral)
    (recurso-disponible Moria        Mithril)
    (recurso-disponible Hobbiton     Alimento)
    
    ;; 2.6. Requisitos de construcción (requiere-recurso)
    (requiere-recurso Extractor1      Mineral)
    (requiere-recurso Fortaleza1      Madera)
    (requiere-recurso Fortaleza1      Mithril)

    ;; 2.7. Conectividad (camino) — bidireccional
    (camino Erebor      Mirkwood)   (camino Mirkwood   Erebor)
    (camino Mirkwood    HighPass)   (camino HighPass   Mirkwood)
    (camino HighPass    Rivendell)  (camino Rivendell  HighPass)
    (camino Rivendell   Moria)      (camino Moria      Rivendell)
    (camino Moria       Lothlorien) (camino Lothlorien Moria)
    (camino Lothlorien  AmonHen)    (camino AmonHen    Lothlorien)
    (camino AmonHen     DeadMarshes)(camino DeadMarshes AmonHen)
    (camino DeadMarshes MinasMorgul)(camino MinasMorgul DeadMarshes)
    (camino MinasMorgul Orodruin)   (camino Orodruin   MinasMorgul)
    (camino MinasMorgul MinasTirith)(camino MinasTirith MinasMorgul)
    (camino MinasTirith Tolfolas)   (camino Tolfolas   MinasTirith)
    (camino Tolfolas    DolAmroth)  (camino DolAmroth  Tolfolas)
    (camino DolAmroth   Edoras)     (camino Edoras     DolAmroth)
    (camino MinasTirith Edoras)     (camino Edoras     MinasTirith)
    (camino Edoras      HelmsDeep)  (camino HelmsDeep  Edoras)
    (camino HelmsDeep   Isengard)   (camino Isengard   HelmsDeep)
    (camino Isengard    Fangorn)    (camino Fangorn    Isengard)
    (camino Fangorn     AmonHen)    (camino AmonHen    Fangorn)
    (camino HelmsDeep   Tharbad)    (camino Tharbad    HelmsDeep)
    (camino Tharbad     Hobbiton)   (camino Hobbiton   Tharbad)
    (camino Hobbiton    Bree)       (camino Bree       Hobbiton)
    (camino Tharbad     Bree)       (camino Bree       Tharbad)
    (camino Bree        Rivendell)  (camino Rivendell  Bree)
  )

  ;; =================== 3. OBJETIVO (:goal) ==========================
  (:goal
    (edificio-construido Fortaleza1 Isengard)
  )
)
