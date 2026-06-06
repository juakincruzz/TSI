; Practica 3 - Ejercicio 4
; Crear un Uruk-Hai en la Torre de Hechiceria de Isengard, ademas de destruir
; el Anillo (como en 3a) y dejar a un humano en Bree.

(define (problem tierra-media-problema4)
  (:domain tierra-media-ej4)

  (:objects
    Hobbit1 Hobbit2 Hobbit3 Hobbit4
    Mago1 Mago2
    Elfo1
    Enano1 Enano2
    Humano1 Humano2
    Orco1 Orco2 Orco3
    Corsario1 Corsario2 - Personaje

    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard HelmsDeep
    Edoras AmonHen MinasTirith DolAmroth Tolfolas
    MinasMorgul DeadMarshes Orodruin - Localizacion
  )

  (:init
    ; -------------------------------------------------------------------------
    ; Tipos de cada personaje.
    ; -------------------------------------------------------------------------
    (personajeEs Hobbit1 Hobbit) (personajeEs Hobbit2 Hobbit)
    (personajeEs Hobbit3 Hobbit) (personajeEs Hobbit4 Hobbit)
    (personajeEs Mago1 Mago)   (personajeEs Mago2 Mago)
    (personajeEs Elfo1 Elfo)
    (personajeEs Enano1 Enano) (personajeEs Enano2 Enano)
    (personajeEs Humano1 Humano) (personajeEs Humano2 Humano)
    (personajeEs Orco1 Orco) (personajeEs Orco2 Orco) (personajeEs Orco3 Orco)
    (personajeEs Corsario1 Corsario) (personajeEs Corsario2 Corsario)

    ; -------------------------------------------------------------------------
    ; Capacidades por tipo (informativo, sirve de documentacion del dominio).
    ; Los Enanos pueden extraer Madera, Mineral y Mithril (asi lo exige el
    ; enunciado), pero el planificador los descartara automaticamente cuando
    ; el recurso vaya a usarse en una construccion "maligna".
    ; -------------------------------------------------------------------------
    (puedeExtraer Enano Madera) (puedeExtraer Enano Mineral) (puedeExtraer Enano Mithril)
    (puedeExtraer Hobbit Alimento)
    (puedeExtraer Orco Mineral) (puedeExtraer Orco Madera)
    (puedeExtraer Humano Madera)
    (puedeExtraer Corsario Especia)

    ; -------------------------------------------------------------------------
    ; Capacidades instanciadas por personaje (precondiciones de ExtraerRecurso).
    ; -------------------------------------------------------------------------
    (puedeExtraerPersonaje Enano1 Madera) (puedeExtraerPersonaje Enano1 Mineral) (puedeExtraerPersonaje Enano1 Mithril)
    (puedeExtraerPersonaje Enano2 Madera) (puedeExtraerPersonaje Enano2 Mineral) (puedeExtraerPersonaje Enano2 Mithril)
    (puedeExtraerPersonaje Hobbit1 Alimento) (puedeExtraerPersonaje Hobbit2 Alimento)
    (puedeExtraerPersonaje Hobbit3 Alimento) (puedeExtraerPersonaje Hobbit4 Alimento)
    (puedeExtraerPersonaje Orco1 Mineral) (puedeExtraerPersonaje Orco1 Madera)
    (puedeExtraerPersonaje Orco2 Mineral) (puedeExtraerPersonaje Orco2 Madera)
    (puedeExtraerPersonaje Orco3 Mineral) (puedeExtraerPersonaje Orco3 Madera)
    (puedeExtraerPersonaje Humano1 Madera) (puedeExtraerPersonaje Humano2 Madera)
    (puedeExtraerPersonaje Corsario1 Especia) (puedeExtraerPersonaje Corsario2 Especia)

    ; -------------------------------------------------------------------------
    ; Recursos necesarios por edificio (para forall + exists de Construir).
    ; -------------------------------------------------------------------------
    (necesita TorreHechiceria Mineral)
    (necesita TorreHechiceria Madera)
    (necesita Extractor Madera)

    ; -------------------------------------------------------------------------
    ; Ubicacion inicial de los personajes.
    ; -------------------------------------------------------------------------
    (en Hobbit1 Hobbiton) (en Hobbit2 Hobbiton) (en Hobbit3 Hobbiton)
    (en Hobbit4 Bree)
    (en Mago1 Rivendell) (en Mago2 Isengard)
    (en Elfo1 Lothlorien)
    (en Enano1 Fangorn) (en Enano2 Erebor)
    (en Humano1 Edoras) (en Humano2 Bree)
    (en Orco1 Moria) (en Orco2 Moria) (en Orco3 Moria)
    (en Corsario1 DolAmroth) (en Corsario2 DolAmroth)

    ; -------------------------------------------------------------------------
    ; Disponibilidad: todos menos Humano1 (ocupado en Edoras).
    ; -------------------------------------------------------------------------
    (disponible Hobbit1) (disponible Hobbit2) (disponible Hobbit3) (disponible Hobbit4)
    (disponible Mago1) (disponible Mago2)
    (disponible Elfo1)
    (disponible Enano1) (disponible Enano2)
    (disponible Humano2)
    (disponible Orco1) (disponible Orco2) (disponible Orco3)
    (disponible Corsario1) (disponible Corsario2)

    ; -------------------------------------------------------------------------
    ; Nodos de recursos (mapa de la Tierra Media).
    ; -------------------------------------------------------------------------
    (recursoEn Mineral Moria) (recursoEn Mithril Moria)
    (recursoEn Mineral Erebor)
    (recursoEn Madera Fangorn) (recursoEn Madera Lothlorien) (recursoEn Madera Mirkwood)
    (recursoEn Alimento Hobbiton)
    (recursoEn Especia Tolfolas)

    ; -------------------------------------------------------------------------
    ; Mision del Anillo (igual que en 3a).
    ; -------------------------------------------------------------------------
    (objetoEn Anillo Rivendell)
    (objetoEn Espada Lothlorien)
    (objetoEsAnillo Anillo)
    (lugarDestruccion Orodruin)
    (puedeRecogerObjeto Hobbit1 Anillo) (puedeRecogerObjeto Hobbit2 Anillo)
    (puedeRecogerObjeto Hobbit3 Anillo) (puedeRecogerObjeto Hobbit4 Anillo)

    ; -------------------------------------------------------------------------
    ; Caminos bidireccionales del mapa.
    ; -------------------------------------------------------------------------
    (camino Hobbiton Bree) (camino Bree Hobbiton)
    (camino Hobbiton Tharbad) (camino Tharbad Hobbiton)
    (camino Bree Tharbad) (camino Tharbad Bree)
    (camino Bree Rivendell) (camino Rivendell Bree)
    (camino Rivendell HighPass) (camino HighPass Rivendell)
    (camino Rivendell Moria) (camino Moria Rivendell)
    (camino HighPass Mirkwood) (camino Mirkwood HighPass)
    (camino Mirkwood Erebor) (camino Erebor Mirkwood)
    (camino Moria Lothlorien) (camino Lothlorien Moria)
    (camino Lothlorien AmonHen) (camino AmonHen Lothlorien)
    (camino Tharbad HelmsDeep) (camino HelmsDeep Tharbad)
    (camino HelmsDeep Isengard) (camino Isengard HelmsDeep)
    (camino Isengard Fangorn) (camino Fangorn Isengard)
    (camino Fangorn AmonHen) (camino AmonHen Fangorn)
    (camino HelmsDeep Edoras) (camino Edoras HelmsDeep)
    (camino Edoras MinasTirith) (camino MinasTirith Edoras)
    (camino Edoras DolAmroth) (camino DolAmroth Edoras)
    (camino DolAmroth Tolfolas) (camino Tolfolas DolAmroth)
    (camino Tolfolas MinasTirith) (camino MinasTirith Tolfolas)
    (camino MinasTirith MinasMorgul) (camino MinasMorgul MinasTirith)
    (camino AmonHen DeadMarshes) (camino DeadMarshes AmonHen)
    (camino DeadMarshes MinasMorgul) (camino MinasMorgul DeadMarshes)
    (camino MinasMorgul Orodruin) (camino Orodruin MinasMorgul)

    ; El camino Rivendell-Moria se destruye al transitarlo.
    (caminoDestructible Rivendell Moria) (caminoDestructible Moria Rivendell)
  )

  ; ---------------------------------------------------------------------------
  ; Objetivo:
  ;   1) Anillo destruido (como en los Ejercicios 2 y 3a).
  ;   2) Un Uruk-Hai creado.
  ;   3) Que algun humano se encuentre en Bree.
  ; ---------------------------------------------------------------------------
  (:goal
    (and
      (anilloDestruido)
      (urukHaiCreado)
      (exists (?h - Personaje)
        (and (personajeEs ?h Humano) (en ?h Bree))
      )
    )
  )
)
