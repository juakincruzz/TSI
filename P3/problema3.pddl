;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; problema3d_3h1m1e.pddl
;; Ejercicio 3D: Comunidad de 3 Hobbits + 1 Mago + 1 Elfo
;;              + 4 Hobbits totales, 2 Magos, 1 Elfo, 2 Enanos, 2 Humanos
;;              -> objetivo: destruir el Anillo.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (problem problema3)
  (:domain dominio3)

  ;; ===================================================================
  ;; 1. OBJETOS (:objects) 
  ;; ===================================================================
  (:objects
    ;; 1) Hobbits (4 totales):
    Hobbit1 Hobbit2 Hobbit3 - Hobbit  ;; en Hobbiton
    Hobbit4                 - Hobbit  ;; en Bree

    ;; 2) Magos (2 totales):
    Mago1 - Mago    ;; en Rivendell
    Mago2 - Mago    ;; en Isengard

    ;; 3) Elfo (1):
    Elfo1 - Elfo    ;; en Lothlorien

    ;; 4) Enanos (2 totales):
    Enano1 - Enano  ;; en Fangorn
    Enano2 - Enano  ;; en Moria

    ;; 5) Humanos (2 totales):
    Humano1 Humano2 - Humano  ;; ambos en Edoras

    ;; 6) Localizaciones (idénticas a ejercicios anteriores):
    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard
    Helms-Deep Edoras Amon-Hen Minas-Tirith Dol-Amroth Tolfolas
    Minas-Morgul Dead-Marshes Orodruin - Localizacion

    ;; 7) Recursos:
    Anillo1  - Anillo   ;; en Rivendell
    Espada1  - Espada   ;; en Lothlorien
    Mithril1 - Mithril  ;; en Moria
  )

  ;; ===================================================================
  ;; 2. INICIALIZACIÓN (:init) 
  ;; ===================================================================
  (:init
    ;; 2.1. Posición INICIAL de PERSONAJES 
    ;;  3 Hobbits en Hobbiton:
    (en Hobbit1 Hobbiton)
    (en Hobbit2 Hobbiton)
    (en Hobbit3 Hobbiton)
    ;;  1 Hobbit en Bree:
    (en Hobbit4 Bree)

    ;;  2 Magos:
    (en Mago1 Rivendell)
    (en Mago2 Isengard)

    ;;  1 Elfo:
    (en Elfo1 Lothlorien)

    ;;  2 Enanos:
    (en Enano1 Fangorn)
    (en Enano2 Moria)

    ;;  2 Humanos:
    (en Humano1 Edoras)
    (en Humano2 Edoras)

    ;; 2.2. Disponibilidad INICIAL 
    (disponible Hobbit1)
    (disponible Hobbit2)
    (disponible Hobbit3)
    (disponible Hobbit4)

    (disponible Mago1)
    (disponible Mago2)

    (disponible Elfo1)

    (disponible Enano1)
    (disponible Enano2)

    (disponible Humano1)
    (disponible Humano2)

    ;; 2.3. Ubicación de RECURSOS 
    (en Anillo1 Rivendell)
    (en Espada1 Lothlorien)
    (en Mithril1 Moria)

    ;; 2.4. Lugar de DESTRUCCIÓN del Anillo 
    (lugarDestruccion Orodruin)

    ;; ===================================================================
    ;; 2.5. Conectividad (todas las aristas bidireccionales igual que antes):
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
  (:goal
    (anilloDestruido)
  )
)
