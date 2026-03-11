;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; problema5.pddl
;; Ejercicio 5: Problema de extracción de Madera en Mirkwood
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(define (problem problema5)
  (:domain dominio5)

  ;; ====================== 1. OBJETOS ============================
  (:objects
    ; Personajes
    Enano1 Enano2   - Enano
    Hobbit1         - Hobbit

    ; Localizaciones
    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard
    Helms-Deep Edoras Amon-Hen Minas-Tirith Dol-Amroth Tolfolas
    Minas-Morgul Dead-Marshes Orodruin            - Localizacion

    ; Nodo de Recurso: único objeto Madera situado en Mirkwood
    MaderaEnMirkwood - Madera
  )

  ;; ====================== 2. INICIALIZACIÓN =====================
  (:init
    ;; Ubicación inicial de Personajes
    (en Enano1 Tharbad)
    (en Enano2 Isengard)
    (en Hobbit1 Isengard)

    ;; Disponibilidad inicial
    (disponible Enano1)
    (disponible Hobbit1)
    ;; Enano2 no aparece como "disponible", por lo que no podrá actuar hasta estar libre.

    ;; Ubicación del recurso Madera
    (en MaderaEnMirkwood Mirkwood)

    ;; Conectividad entre Localizaciones (bidireccional)
    (camino Tharbad Helms-Deep) (camino Helms-Deep Tharbad)
    (camino Bree Rivendell)     (camino Rivendell Bree)
    (camino Bree Tharbad)       (camino Tharbad Bree)
    (camino Hobbiton Bree)      (camino Bree Hobbiton)
    (camino Rivendell HighPass) (camino HighPass Rivendell)
    (camino Rivendell Moria)    (camino Moria Rivendell)
    (camino HighPass Mirkwood)  (camino Mirkwood HighPass)
    (camino Mirkwood Erebor)    (camino Erebor Mirkwood)
    (camino Moria Lothlorien)   (camino Lothlorien Moria)
    (camino Lothlorien Amon-Hen) (camino Amon-Hen Lothlorien)
    (camino Amon-Hen Dead-Marshes) (camino Dead-Marshes Amon-Hen)
    (camino Amon-Hen Fangorn)   (camino Fangorn Amon-Hen)
    (camino Minas-Morgul Dead-Marshes) (camino Dead-Marshes Minas-Morgul)
    (camino Minas-Morgul Orodruin)     (camino Orodruin Minas-Morgul)
    (camino Minas-Tirith Minas-Morgul) (camino Minas-Morgul Minas-Tirith)
    (camino Minas-Tirith Tolfolas)     (camino Tolfolas Minas-Tirith)
    (camino Dol-Amroth Tolfolas)       (camino Tolfolas Dol-Amroth)
    (camino Edoras Minas-Tirith)       (camino Minas-Tirith Edoras)
    (camino Dol-Amroth Edoras)         (camino Edoras Dol-Amroth)
    (camino Helms-Deep Edoras)         (camino Edoras Helms-Deep)
    (camino Isengard Helms-Deep)       (camino Helms-Deep Isengard)
    (camino Fangorn Isengard)          (camino Isengard Fangorn)

    ;; Inicialización de la función de coste acumulado
    (= (total-cost) 0)

    ;; Costes de viajes: coste 3 para rutas específicas
    (= (coste-camino Tharbad Helms-Deep) 3)
    (= (coste-camino Helms-Deep Tharbad) 3)
    (= (coste-camino Lothlorien Amon-Hen) 3)
    (= (coste-camino Amon-Hen Lothlorien) 3)

    ;; Coste unitario (1) para el resto de caminos
    (= (coste-camino Bree Rivendell) 1)      (= (coste-camino Rivendell Bree) 1)
    (= (coste-camino Bree Tharbad) 1)        (= (coste-camino Tharbad Bree) 1)
    (= (coste-camino Hobbiton Bree) 1)       (= (coste-camino Bree Hobbiton) 1)
    (= (coste-camino Rivendell HighPass) 1)  (= (coste-camino HighPass Rivendell) 1)
    (= (coste-camino Rivendell Moria) 1)     (= (coste-camino Moria Rivendell) 1)
    (= (coste-camino HighPass Mirkwood) 1)   (= (coste-camino Mirkwood HighPass) 1)
    (= (coste-camino Mirkwood Erebor) 1)     (= (coste-camino Erebor Mirkwood) 1)
    (= (coste-camino Moria Lothlorien) 1)    (= (coste-camino Lothlorien Moria) 1)
    (= (coste-camino Amon-Hen Dead-Marshes) 1) (= (coste-camino Dead-Marshes Amon-Hen) 1)
    (= (coste-camino Amon-Hen Fangorn) 1)     (= (coste-camino Fangorn Amon-Hen) 1)
    (= (coste-camino Minas-Morgul Dead-Marshes) 1) (= (coste-camino Dead-Marshes Minas-Morgul) 1)
    (= (coste-camino Minas-Morgul Orodruin) 1)     (= (coste-camino Orodruin Minas-Morgul) 1)
    (= (coste-camino Minas-Tirith Minas-Morgul) 1) (= (coste-camino Minas-Morgul Minas-Tirith) 1)
    (= (coste-camino Minas-Tirith Tolfolas) 1)     (= (coste-camino Tolfolas Minas-Tirith) 1)
    (= (coste-camino Dol-Amroth Tolfolas) 1)       (= (coste-camino Tolfolas Dol-Amroth) 1)
    (= (coste-camino Edoras Minas-Tirith) 1)       (= (coste-camino Minas-Tirith Edoras) 1)
    (= (coste-camino Dol-Amroth Edoras) 1)         (= (coste-camino Edoras Dol-Amroth) 1)
    (= (coste-camino Helms-Deep Edoras) 1)         (= (coste-camino Edoras Helms-Deep) 1)
    (= (coste-camino Isengard Helms-Deep) 1)       (= (coste-camino Helms-Deep Isengard) 1)
    (= (coste-camino Fangorn Isengard) 1)          (= (coste-camino Isengard Fangorn) 1)
  )

  ;; ====================== 3. META (:goal) =========================
  (:goal
    (trabajando Enano1 Mirkwood MaderaEnMirkwood)
  )

  ;; ====================== 4. MÉTRICA ==============================
  (:metric minimize (total-cost))
)
