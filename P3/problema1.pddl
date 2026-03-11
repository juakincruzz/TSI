;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; problema1.pddl
;; Ejercicio 1: “Modelo base”
;;
;; - 2 Enanos y 1 Hobbit:
;;     - Enano1 en Tharbad (disponible)
;;     - Enano2 en Isengard (no disponible)
;;     - Hobbit1 en Isengard (disponible)
;; - Mapa completo de Localizaciones según el enunciado,
;;   incluyendo la arista Tharbad <-> Helm’s-Deep.
;; - Un único nodo de Recurso de tipo Madera (en Fangorn)
;; - Objetivo: el Enano1 debe estar “trabajando” en el nodo MaderaFangorn.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (problem problema1)
  (:domain dominio1)

  ;; ====================================================================
  ;; 1. OBJETOS (:objects)
  ;; ====================================================================
  (:objects
    ;; Personajes
    Enano1 Enano2 - Enano
    Hobbit1       - Hobbit

    ;; Localizaciones
    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard
    Helms-Deep Edoras Amon-Hen Minas-Tirith Dol-Amroth Tolfolas
    Minas-Morgul Dead-Marshes Orodruin - Localizacion

    ;; Nodo de Recurso: Madera en Fangorn
    MaderaFangorn - Madera
  )

  ;; ===================================================================
  ;; 2. INICIALIZACIÓN (:init) 
  ;; ===================================================================
  (:init
    ;; Posición inicial de Personajes 
    (en Enano1 Tharbad)
    (en Enano2 Isengard)
    (en Hobbit1 Isengard)

    ;; Disponibilidad inicial 
    (disponible Enano1)
    (disponible Hobbit1)
    ;; Enano2 NO está disponible -> no incluimos “(disponible Enano2)”

    ;; Nodo de Recurso 
    (en MaderaFangorn Fangorn)

    ;; ===================================================================
    ;;  Conectividad (aristas bidireccionales según la lista recibida):
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
  ;; Objetivo concreto (sin cuantificadores) para evitar axiomas:
  ;; que Enano1 acabe “trabajando” en MaderaFangorn.
  (:goal
    (trabajando Enano1 Fangorn MaderaFangorn)
  )
)
