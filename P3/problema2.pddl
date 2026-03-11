;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; problema2.pddl
;; Ejercicio 2: “Formar Comunidad reducida y destruir el Anillo”
;;
;; 1) Personajes:
;;    - Hobbits:   Hobbit1, Hobbit2, Hobbit3 (en Hobbiton),   Hobbit4 (en Bree)
;;    - Magos:     Mago1 (en Rivendell),                     Mago2 (en Isengard)
;;    - Enano1 (en Fangorn), pero NO disponible (deja al Enano1 trabajando en madera).
;;
;;    De todos ellos, solo los Hobbits y los Magos estarán “disponibles” inicialmente.
;;
;; 2) Recursos (nodos):
;;    - Anillo1   (tipo Anillo, en Rivendell)
;;    - Espada1   (tipo Espada, en Lothlorien)
;;    - Mithril1  (tipo Mithril, en Moria)
;;    - (No necesitamos nodos de Madera o Alimento para este ejercicio.)
;;
;; 3) Localizaciones: las mismas 20 del mapa.
;;
;; 4) Predicado fijo:
;;    (lugarDestruccion Orodruin)  ; definimos dónde debe invocarse destruirAnillo
;;
;; 5) Meta:
;;    (anilloDestruido)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (problem problema2)
  (:domain dominio2)

  ;; ===================================================================
  ;; 1. OBJETOS (:objects)
  ;; ===================================================================
  (:objects
    ;; Personajes
    Hobbit1 Hobbit2 Hobbit3 Hobbit4 - Hobbit
    Mago1   Mago2                   - Mago
    Enano1                         - Enano

    ;; Localizaciones (idénticas a ejercicio 1)
    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard
    Helms-Deep Edoras Amon-Hen Minas-Tirith Dol-Amroth Tolfolas
    Minas-Morgul Dead-Marshes Orodruin - Localizacion

    ;; Recursos
    Anillo1  - Anillo
    Espada1  - Espada
    Mithril1 - Mithril
  )

  ;; ===================================================================
  ;; 2. INICIALIZACIÓN (:init) 
  ;; ===================================================================
  (:init
    ;; Posición inicial de PERSONAJES 
    (en Hobbit1 Hobbiton)
    (en Hobbit2 Hobbiton)
    (en Hobbit3 Hobbiton)
    (en Hobbit4 Bree)

    (en Mago1 Rivendell)
    (en Mago2 Isengard)

    (en Enano1 Fangorn)  ;; permanece “ocupado” (no disponible)

    ;; Disponibilidad inicial
    (disponible Hobbit1)
    (disponible Hobbit2)
    (disponible Hobbit3)
    (disponible Hobbit4)
    (disponible Mago1)
    (disponible Mago2)
    ;; Enano1 NO está disponible -> no ponemos (disponible Enano1)

    ;; Recursos: ubicaciones de nodos 
    (en Anillo1 Rivendell)
    (en Espada1 Lothlorien)
    (en Mithril1 Moria)

    ;; Predicado especial: lugar de destrucción del Anillo 
    (lugarDestruccion Orodruin)

    ;; ===================================================================
    ;;  Conectividad (mismas aristas bidireccionales que en ejercicio 1):
    ;; ===================================================================
    (camino Tharbad Helms-Deep)
    (camino Helms-Deep Tharbad)

    (camino Bree Rivendell)
    (camino Rivendell Bree)

    (camino Bree Tharbad)
    (camino Tharbad Bree)

    (camino Hobbiton Bree)
    (camino Bree Hobbiton)

    (camino Rivendell HighPass)
    (camino HighPass Rivendell)

    (camino Rivendell Moria)
    (camino Moria Rivendell)

    (camino HighPass Mirkwood)
    (camino Mirkwood HighPass)

    (camino Mirkwood Erebor)
    (camino Erebor Mirkwood)

    (camino Moria Lothlorien)
    (camino Lothlorien Moria)

    (camino Lothlorien Amon-Hen)
    (camino Amon-Hen Lothlorien)

    (camino Amon-Hen Dead-Marshes)
    (camino Dead-Marshes Amon-Hen)

    (camino Amon-Hen Fangorn)
    (camino Fangorn Amon-Hen)

    (camino Minas-Morgul Dead-Marshes)
    (camino Dead-Marshes Minas-Morgul)

    (camino Minas-Morgul Orodruin)
    (camino Orodruin Minas-Morgul)

    (camino Minas-Tirith Minas-Morgul)
    (camino Minas-Morgul Minas-Tirith)

    (camino Minas-Tirith Tolfolas)
    (camino Tolfolas Minas-Tirith)

    (camino Dol-Amroth Tolfolas)
    (camino Tolfolas Dol-Amroth)

    (camino Edoras Minas-Tirith)
    (camino Minas-Tirith Edoras)

    (camino Dol-Amroth Edoras)
    (camino Edoras Dol-Amroth)

    (camino Helms-Deep Edoras)
    (camino Edoras Helms-Deep)

    (camino Isengard Helms-Deep)
    (camino Helms-Deep Isengard)

    (camino Fangorn Isengard)
    (camino Isengard Fangorn)
  )

  ;; ===================================================================
  ;; 3. META (:goal) 
  ;; ===================================================================
  ;; El objetivo final es destruir el Anillo Único:
  (:goal
    (anilloDestruido)
  )
)
