; Practica 3 - Ejercicio 3d
; Estado inicial comun del Ejercicio 3. Comunidad: 3 Hobbits, 1 Mago y 1 Elfo.
;
; Este problema mantiene la configuracion de 3c y anade el requisito de incluir
; un Elfo en la Comunidad. El Elfo incrementa los parametros de las acciones de
; Comunidad y hace visible el crecimiento combinatorio del problema.

(define (problem tierra-media-problema3d)
  (:domain tierra-media-ej3d)

  (:objects
    ; Personajes disponibles en el escenario. No todos acaban necesariamente en
    ; la Comunidad: el dominio de cada apartado fija cuantos Hobbits, Magos y
    ; Elfos deben incorporarse.
    Hobbit1 Hobbit2 Hobbit3 Hobbit4
    Mago1 Mago2
    Elfo1
    Enano1 Enano2 - Personaje

    ; Localizaciones del mapa de la Tierra Media usadas en todos los apartados
    ; del Ejercicio 3.
    Hobbiton Bree Rivendell HighPass Mirkwood Erebor
    Moria Lothlorien Tharbad Fangorn Isengard HelmsDeep
    Edoras AmonHen MinasTirith DolAmroth Tolfolas
    MinasMorgul DeadMarshes Orodruin - Localizacion
  )

  (:init
    ; Tipos de cada personaje. Los tipos se representan como constantes del
    ; dominio y se asignan aqui mediante personajeEs.
    (personajeEs Hobbit1 Hobbit) (personajeEs Hobbit2 Hobbit) (personajeEs Hobbit3 Hobbit) (personajeEs Hobbit4 Hobbit)
    (personajeEs Mago1 Mago) (personajeEs Mago2 Mago)
    (personajeEs Elfo1 Elfo)
    (personajeEs Enano1 Enano) (personajeEs Enano2 Enano)

    ; Capacidades de extraccion por tipo y por personaje. Las capacidades
    ; instanciadas son las que usa directamente la accion ExtraerRecurso.
    (puedeExtraer Enano Madera) (puedeExtraer Enano Mineral) (puedeExtraer Enano Mithril)
    (puedeExtraer Hobbit Alimento)
    (puedeExtraerPersonaje Enano1 Madera) (puedeExtraerPersonaje Enano1 Mineral) (puedeExtraerPersonaje Enano1 Mithril)
    (puedeExtraerPersonaje Enano2 Madera) (puedeExtraerPersonaje Enano2 Mineral) (puedeExtraerPersonaje Enano2 Mithril)
    (puedeExtraerPersonaje Hobbit1 Alimento) (puedeExtraerPersonaje Hobbit2 Alimento)
    (puedeExtraerPersonaje Hobbit3 Alimento) (puedeExtraerPersonaje Hobbit4 Alimento)

    ; Posiciones iniciales. Los Hobbits empiezan repartidos entre Hobbiton y
    ; Bree; Mago1 parte de Rivendell, lo que lo convierte en candidato natural
    ; para recoger el Anillo con la Comunidad.
    (en Hobbit1 Hobbiton) (en Hobbit2 Hobbiton) (en Hobbit3 Hobbiton) (en Hobbit4 Bree)
    (en Mago1 Rivendell) (en Mago2 Isengard) (en Elfo1 Lothlorien)
    (en Enano1 Moria) (en Enano2 Fangorn)
    (disponible Hobbit1) (disponible Hobbit2) (disponible Hobbit3) (disponible Hobbit4)
    (disponible Mago1) (disponible Mago2) (disponible Elfo1) (disponible Enano1) (disponible Enano2)

    ; Nodos de recursos. El Mithril de Moria permite materializar el Chaleco;
    ; el resto de recursos se conservan para mantener el escenario comun.
    (recursoEn Mineral Moria) (recursoEn Mithril Moria) (recursoEn Mineral Erebor)
    (recursoEn Madera Fangorn) (recursoEn Madera Lothlorien) (recursoEn Madera Mirkwood)
    (recursoEn Alimento Hobbiton) (recursoEn Especia Tolfolas)

    ; Objetos de la mision del Anillo. El ChalecoMithril no aparece inicialmente:
    ; se crea mediante materializarChaleco cuando hay Mithril generado.
    (objetoEn Anillo Rivendell) (objetoEn Espada Lothlorien)
    (objetoEsAnillo Anillo) (lugarDestruccion Orodruin)
    (puedeRecogerObjeto Hobbit1 Anillo) (puedeRecogerObjeto Hobbit2 Anillo)
    (puedeRecogerObjeto Hobbit3 Anillo) (puedeRecogerObjeto Hobbit4 Anillo)
    ; Orden canonico entre Hobbits para romper simetrias. El Elfo no necesita
    ; una relacion equivalente porque solo existe Elfo1 en el problema.
    (hobbitAntes Hobbit1 Hobbit2) (hobbitAntes Hobbit1 Hobbit3) (hobbitAntes Hobbit1 Hobbit4)
    (hobbitAntes Hobbit2 Hobbit3) (hobbitAntes Hobbit2 Hobbit4)
    (hobbitAntes Hobbit3 Hobbit4)
    ; Caminos bidireccionales del mapa. Se declaran ambos sentidos de cada
    ; arista para que Viajar y viajarComunidad puedan moverse en ida y vuelta.
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

    ; El camino Rivendell-Moria se destruye al transitarlo, igual que en los
    ; ejercicios anteriores. El dominio elimina ambos sentidos mediante when.
    (caminoDestructible Rivendell Moria) (caminoDestructible Moria Rivendell)
  )

  ; Objetivo: destruir el Anillo. El dominio fuerza que, para lograrlo, el
  ; portador llegue a Orodruin con Anillo, ChalecoMithril y Espada.
  (:goal (anilloDestruido))
)
