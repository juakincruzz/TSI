; Practica 3 - Ejercicio 5
; Problema identico al Ejercicio 1 (mismos personajes, mapa y objetivo),
; con la adicion de costes variables en los desplazamientos.
;
; Todos los tramos tienen coste 1 salvo los indicados en el enunciado:
;   Tharbad     - HelmsDeep:    3
;   MinasMorgul - Orodruin:     3
;   Rivendell   - HighPass:     2
;   Rivendell   - Moria:        3
;   Lothlorien  - AmonHen:      5
;   HighPass    - Mirkwood:     2
;   Mirkwood    - Erebor:       2
;   Moria       - Lothlorien:   3
;   Fangorn     - AmonHen:      8
;   AmonHen     - DeadMarshes:  2
;   DeadMarshes - MinasMorgul:  2
;   MinasTirith - MinasMorgul:  2
;   Edoras      - MinasTirith:  2
;   MinasTirith - Tolfolas:     2
;   Edoras      - DolAmroth:    2
; El coste indicado afecta a ambas direcciones (ida y vuelta).

(define (problem tierra-media-problema5)
  (:domain tierra-media-ej5)

  (:objects
    Enano1 Enano2 Hobbit1 - Personaje

    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard HelmsDeep
    Edoras AmonHen MinasTirith DolAmroth Tolfolas
    MinasMorgul DeadMarshes Orodruin - Localizacion
  )

  (:init
    ; -------------------------------------------------------------------------
    ; Coste inicial acumulado.
    ; -------------------------------------------------------------------------
    (= (total-cost) 0)

    ; -------------------------------------------------------------------------
    ; Tipos de personaje.
    ; -------------------------------------------------------------------------
    (personajeEs Enano1 Enano)
    (personajeEs Enano2 Enano)
    (personajeEs Hobbit1 Hobbit)

    ; -------------------------------------------------------------------------
    ; Capacidades por tipo de personaje.
    ; -------------------------------------------------------------------------
    (puedeExtraer Enano Madera)
    (puedeExtraer Enano Mineral)
    (puedeExtraer Enano Mithril)
    (puedeExtraer Hobbit Alimento)

    ; -------------------------------------------------------------------------
    ; Capacidades instanciadas por personaje.
    ; -------------------------------------------------------------------------
    (puedeExtraerPersonaje Enano1 Madera)
    (puedeExtraerPersonaje Enano1 Mineral)
    (puedeExtraerPersonaje Enano1 Mithril)
    (puedeExtraerPersonaje Enano2 Madera)
    (puedeExtraerPersonaje Enano2 Mineral)
    (puedeExtraerPersonaje Enano2 Mithril)
    (puedeExtraerPersonaje Hobbit1 Alimento)

    ; -------------------------------------------------------------------------
    ; Estado inicial de los personajes.
    ; -------------------------------------------------------------------------
    (en Enano1 Tharbad)
    (en Hobbit1 Lothlorien)
    (en Enano2 Isengard)
    (disponible Enano1)
    (disponible Hobbit1)
    ; Enano2 no esta disponible (mision diplomatica).

    ; -------------------------------------------------------------------------
    ; Nodos de recursos.
    ; -------------------------------------------------------------------------
    (recursoEn Mineral Moria)
    (recursoEn Mithril Moria)
    (recursoEn Mineral Erebor)
    (recursoEn Madera Fangorn)
    (recursoEn Madera Lothlorien)
    (recursoEn Madera Mirkwood)
    (recursoEn Alimento Hobbiton)
    (recursoEn Especia Tolfolas)

    ; -------------------------------------------------------------------------
    ; Caminos bidireccionales del mapa.
    ; -------------------------------------------------------------------------
    (camino Hobbiton Bree)       (camino Bree Hobbiton)
    (camino Hobbiton Tharbad)    (camino Tharbad Hobbiton)
    (camino Bree Tharbad)        (camino Tharbad Bree)
    (camino Bree Rivendell)      (camino Rivendell Bree)
    (camino Rivendell HighPass)  (camino HighPass Rivendell)
    (camino Rivendell Moria)     (camino Moria Rivendell)
    (camino HighPass Mirkwood)   (camino Mirkwood HighPass)
    (camino Mirkwood Erebor)     (camino Erebor Mirkwood)
    (camino Moria Lothlorien)    (camino Lothlorien Moria)
    (camino Lothlorien AmonHen)  (camino AmonHen Lothlorien)
    (camino Tharbad HelmsDeep)   (camino HelmsDeep Tharbad)
    (camino HelmsDeep Isengard)  (camino Isengard HelmsDeep)
    (camino Isengard Fangorn)    (camino Fangorn Isengard)
    (camino Fangorn AmonHen)     (camino AmonHen Fangorn)
    (camino HelmsDeep Edoras)    (camino Edoras HelmsDeep)
    (camino Edoras MinasTirith)  (camino MinasTirith Edoras)
    (camino Edoras DolAmroth)    (camino DolAmroth Edoras)
    (camino DolAmroth Tolfolas)  (camino Tolfolas DolAmroth)
    (camino Tolfolas MinasTirith)    (camino MinasTirith Tolfolas)
    (camino MinasTirith MinasMorgul) (camino MinasMorgul MinasTirith)
    (camino AmonHen DeadMarshes)    (camino DeadMarshes AmonHen)
    (camino DeadMarshes MinasMorgul) (camino MinasMorgul DeadMarshes)
    (camino MinasMorgul Orodruin)    (camino Orodruin MinasMorgul)

    ; El camino Rivendell-Moria se destruye al transitarlo.
    (caminoDestructible Rivendell Moria)
    (caminoDestructible Moria Rivendell)

    ; -------------------------------------------------------------------------
    ; Costes de desplazamiento por tramo (bidireccionales).
    ; Tramos con coste 1 (por defecto segun el enunciado).
    ; -------------------------------------------------------------------------
    (= (costeCamino Hobbiton Bree) 1)      (= (costeCamino Bree Hobbiton) 1)
    (= (costeCamino Hobbiton Tharbad) 1)   (= (costeCamino Tharbad Hobbiton) 1)
    (= (costeCamino Bree Tharbad) 1)       (= (costeCamino Tharbad Bree) 1)
    (= (costeCamino Bree Rivendell) 1)     (= (costeCamino Rivendell Bree) 1)
    (= (costeCamino HelmsDeep Isengard) 1) (= (costeCamino Isengard HelmsDeep) 1)
    (= (costeCamino Isengard Fangorn) 1)   (= (costeCamino Fangorn Isengard) 1)
    (= (costeCamino HelmsDeep Edoras) 1)   (= (costeCamino Edoras HelmsDeep) 1)
    (= (costeCamino DolAmroth Tolfolas) 1) (= (costeCamino Tolfolas DolAmroth) 1)

    ; -------------------------------------------------------------------------
    ; Tramos con coste distinto de 1 (segun el enunciado).
    ; -------------------------------------------------------------------------
    (= (costeCamino Tharbad HelmsDeep) 3)      (= (costeCamino HelmsDeep Tharbad) 3)
    (= (costeCamino MinasMorgul Orodruin) 3)   (= (costeCamino Orodruin MinasMorgul) 3)
    (= (costeCamino Rivendell HighPass) 2)     (= (costeCamino HighPass Rivendell) 2)
    (= (costeCamino Rivendell Moria) 3)        (= (costeCamino Moria Rivendell) 3)
    (= (costeCamino Lothlorien AmonHen) 5)     (= (costeCamino AmonHen Lothlorien) 5)
    (= (costeCamino HighPass Mirkwood) 2)      (= (costeCamino Mirkwood HighPass) 2)
    (= (costeCamino Mirkwood Erebor) 2)        (= (costeCamino Erebor Mirkwood) 2)
    (= (costeCamino Moria Lothlorien) 3)       (= (costeCamino Lothlorien Moria) 3)
    (= (costeCamino Fangorn AmonHen) 8)        (= (costeCamino AmonHen Fangorn) 8)
    (= (costeCamino AmonHen DeadMarshes) 2)    (= (costeCamino DeadMarshes AmonHen) 2)
    (= (costeCamino DeadMarshes MinasMorgul) 2) (= (costeCamino MinasMorgul DeadMarshes) 2)
    (= (costeCamino MinasTirith MinasMorgul) 2) (= (costeCamino MinasMorgul MinasTirith) 2)
    (= (costeCamino Edoras MinasTirith) 2)     (= (costeCamino MinasTirith Edoras) 2)
    (= (costeCamino MinasTirith Tolfolas) 2)   (= (costeCamino Tolfolas MinasTirith) 2)
    (= (costeCamino Edoras DolAmroth) 2)       (= (costeCamino DolAmroth Edoras) 2)
  )

  ; ---------------------------------------------------------------------------
  ; Objetivo: identico al Ejercicio 1.
  ; ---------------------------------------------------------------------------
  (:goal
    (and
      (recursoGenerado Mithril)
      (recursoGenerado Alimento)
    )
  )

  ; ---------------------------------------------------------------------------
  ; Metrica: minimizar el coste total de desplazamiento.
  ; ---------------------------------------------------------------------------
  (:metric minimize (total-cost))
)
