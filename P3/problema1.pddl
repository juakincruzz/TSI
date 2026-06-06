; Practica 3 - Ejercicio 1
; Problema: conseguir que se genere Mithril y Alimento.

(define (problem tierra-media-problema1)
  (:domain tierra-media-ej1)

  (:objects
    Enano1 Enano2 Hobbit1 - Personaje

    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard HelmsDeep
    Edoras AmonHen MinasTirith DolAmroth Tolfolas
    MinasMorgul DeadMarshes Orodruin - Localizacion
  )

  (:init
    ; Tipos de personaje.
    (personajeEs Enano1 Enano)
    (personajeEs Enano2 Enano)
    (personajeEs Hobbit1 Hobbit)

    ; Capacidades por tipo de personaje.
    (puedeExtraer Enano Madera)
    (puedeExtraer Enano Mineral)
    (puedeExtraer Enano Mithril)
    (puedeExtraer Hobbit Alimento)

    ; Capacidades instanciadas por personaje, coherentes con personajeEs y
    ; puedeExtraer. Se incluyen para no cambiar la signatura de ExtraerRecurso.
    (puedeExtraerPersonaje Enano1 Madera)
    (puedeExtraerPersonaje Enano1 Mineral)
    (puedeExtraerPersonaje Enano1 Mithril)
    (puedeExtraerPersonaje Enano2 Madera)
    (puedeExtraerPersonaje Enano2 Mineral)
    (puedeExtraerPersonaje Enano2 Mithril)
    (puedeExtraerPersonaje Hobbit1 Alimento)

    ; Estado inicial de los personajes.
    (en Enano1 Tharbad)
    (en Hobbit1 Lothlorien)
    (en Enano2 Isengard)
    (disponible Enano1)
    (disponible Hobbit1)
    ; Enano2 no esta disponible porque esta en una mision diplomatica.

    ; Nodos de recursos.
    (recursoEn Mineral Moria)
    (recursoEn Mithril Moria)
    (recursoEn Mineral Erebor)
    (recursoEn Madera Fangorn)
    (recursoEn Madera Lothlorien)
    (recursoEn Madera Mirkwood)
    (recursoEn Alimento Hobbiton)
    (recursoEn Especia Tolfolas)

    ; Caminos bidireccionales del mapa.
    (camino Hobbiton Bree)
    (camino Bree Hobbiton)
    (camino Hobbiton Tharbad)
    (camino Tharbad Hobbiton)
    (camino Bree Tharbad)
    (camino Tharbad Bree)
    (camino Bree Rivendell)
    (camino Rivendell Bree)
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
    (camino Lothlorien AmonHen)
    (camino AmonHen Lothlorien)
    (camino Tharbad HelmsDeep)
    (camino HelmsDeep Tharbad)
    (camino HelmsDeep Isengard)
    (camino Isengard HelmsDeep)
    (camino Isengard Fangorn)
    (camino Fangorn Isengard)
    (camino Fangorn AmonHen)
    (camino AmonHen Fangorn)
    (camino HelmsDeep Edoras)
    (camino Edoras HelmsDeep)
    (camino Edoras MinasTirith)
    (camino MinasTirith Edoras)
    (camino Edoras DolAmroth)
    (camino DolAmroth Edoras)
    (camino DolAmroth Tolfolas)
    (camino Tolfolas DolAmroth)
    (camino Tolfolas MinasTirith)
    (camino MinasTirith Tolfolas)
    (camino MinasTirith MinasMorgul)
    (camino MinasMorgul MinasTirith)
    (camino AmonHen DeadMarshes)
    (camino DeadMarshes AmonHen)
    (camino DeadMarshes MinasMorgul)
    (camino MinasMorgul DeadMarshes)
    (camino MinasMorgul Orodruin)
    (camino Orodruin MinasMorgul)

    ; El camino Rivendell-Moria se destruye al transitarlo.
    (caminoDestructible Rivendell Moria)
    (caminoDestructible Moria Rivendell)
  )

  (:goal
    (and
      (recursoGenerado Mithril)
      (recursoGenerado Alimento)
    )
  )
)
