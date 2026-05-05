% ============================================================
% FICHERO    : Ejercicio2.mzn
%
% TIPO       : CSP (solve satisfy)
%
% VARIABLES DE DECISIÓN:
%   proyecto[e] - Proyecto asignado al estudiante e (enum Proyecto).
%   aula[e]     - Aula asignada al estudiante e (enum Aula).
%   hora[e]     - Slot horario del estudiante e (1..6 -> 15:00–20:00).
%
% RESTRICCIONES PRINCIPALES:
%   · all_different(proyecto), all_different(aula), all_different(hora).
%   · Bruno presenta en el slot 1 (15:00).
%   · P4 en aula B2 y antes que P1.
%   · P1 inmediatamente antes que P3; aula de P1 != B2 ni A1.
%   · Diego exactamente 1 hora después que Alba.
%   · Carla exactamente 1 hora después que Diego.
%   · Elena en C1, después de Carla y antes de Pablo.
%   · P2 exactamente 1 hora después que P3; aula de P2 != A2.
%   · P3 en aula A2.
%   · P5 a las 19:00 (slot 5); P6 en C2 a las 20:00 (slot 6).
% ============================================================

include "globals.mzn";

% ============================================================
% DATOS DEL PROBLEMA
% ============================================================
% enums para tratar a los Estudiantes, Proyectos y Aulas.
enum Estudiante = { Alba, Bruno, Carla, Diego, Elena, Pablo } ;
enum Proyecto   = { P1, P2, P3, P4, P5, P6 }                  ;
enum Aula       = { A1, A2, B1, B2, C1, C2 }                  ;

% Nombres de proyectos (para el output)
array[Proyecto] of string: nombreProyecto = [
    "A* cuantico",
    "Demostracion matematica de que RTA* es un algoritmo absurdo",
    "Modelado de la incertidumbre a la hora de aprobar TSI sin estudiar nada",
    "Poda alfa-beta aplicada al encaje de bolillos",
    "Planificacion automatica jerarquica hibrida dedicada a la yuxtaposicion de los mundos paralelos",
    "Brazo robotico para jugar a la pocha"
];

% Solo para el output, uso un array de Strings que convierte a entero -> hora real.
array[1..6] of string : nombreHora = [ "15:00", "16:00", "17:00", "18:00", "19:00", "20:00" ] ;


% VARIABLES DE DECISIÓN
% Arrays de estudiante para cada caso.
array[Estudiante] of var Proyecto: proyecto ;
array[Estudiante] of var Aula:     aula     ;
array[Estudiante] of var 1..6:     hora     ;


% RESTRICCIONES

% Cada proyecto, aula y hora debe asignarse a un único estudiante.

% Restriccion de proyecto, un estudiante, un proyecto.
constraint all_different(proyecto) ;

% Restriccion de aula, un estudiante, una aula.
constraint all_different(aula)     ;

% Restriccion de hora, un estudiante, una hora.
constraint all_different(hora)     ;

% Frases del enunciado
% Bruno es el primero en presentar
constraint hora[Bruno] = 1 ;

% El proyecto 4 se presenta en el aula B2 y antes que el proyecto 1.
% Lo divido en dos restricciones, la primera el aula y la segunda la hora
constraint forall(e in Estudiante)(
  proyecto[e] = P4 -> aula[e] = B2
);

constraint forall(e1, e2 in Estudiante)(
  proyecto[e1] = P4 /\ proyecto[e2] = P1 -> hora[e1] < hora[e2]
);

% El proyecto 1 se presenta inmediatamente antes que el proyecto 3, en un aula que no es ni la B2
% ni la A1.
% Lo divido en dos restricciones de nuevo, la primera la hora y la segunda las aulas
constraint forall(e1, e2 in Estudiante)(
  proyecto[e1] = P1 /\ proyecto[e2] = P3 -> hora[e1] + 1 = hora[e2]
);

constraint forall(e in Estudiante)(
  proyecto[e] = P1 -> aula[e] != B2 /\ aula[e] != A1
);

% Diego presenta exactamente una hora después que Alba.
constraint hora[Alba] + 1 = hora[Diego] ;

% Carla presenta exactamente una hora después que Diego.
constraint hora[Diego] + 1 = hora[Carla] ;

% Elena presenta en el aula C1, y su presentación tiene lugar después de Carla pero antes de Pablo.
% Restriccion 1, aula de Elena.
constraint aula[Elena] = C1 ;

% Restriccion 2, hora con respecto a Carla
constraint hora[Elena] > hora[Carla] ;

% Restriccion 3, hora con respecto a Pablo
constraint hora[Elena] < hora[Pablo] ;

% El proyecto 2 se presenta exactamente una hora después que el proyecto 3, en un aula que no es
% la A2.
% Restriccion 1, hora de proyectos
constraint forall(e1, e2 in Estudiante)(
  proyecto[e1] = P3 /\ proyecto[e2] = P2 -> hora[e1] + 1 = hora[e2]
);

% Restriccion 2, aula
constraint forall(e in Estudiante)(
  proyecto[e] = P2 -> aula[e] != A2
);

% El proyecto 3 se presenta en el aula A2.
constraint forall(e in Estudiante)(
  proyecto[e] = P3 -> aula[e] = A2
);

% El proyecto 5 se presenta a las 19:00, mientras que el proyecto 6 se presenta en la C2 a última
% hora.
constraint forall(e in Estudiante)(
  proyecto[e] = P5 -> hora[e] = 5
);

constraint forall(e in Estudiante)(
  proyecto[e] = P6 -> aula[e] = C2 /\ hora[e] = 6
);


% RESOLVER: CSP (satisfy)
solve satisfy;


% OUTPUT
output [
    "\"" ++ show(e) ++ "\": \"" ++
    nombreProyecto[fix(proyecto[e])] ++ "\", \"" ++
    show(fix(aula[e])) ++ "\", \"" ++
    nombreHora[fix(hora[e])] ++ "\"\n"
    | e in Estudiante
];
