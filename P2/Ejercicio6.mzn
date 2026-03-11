% Modelo para la conformación de tribunales de TFG

% Definición de tipos y constantes
enum DEPTS = {DECSAI, LSI, ICAR};
enum DIAS = {LUNES, MARTES, MIERCOLES, JUEVES, VIERNES};
enum TRIBUNALES = {T1, T2};
set of int: PROFESORES_DECSAI = 1..3;
set of int: PROFESORES_LSI = 4..6;
set of int: PROFESORES_ICAR = 7..9;
set of int: PROFESORES = 1..9;
set of int: TFGS = 1..10;

% Variables
array[TRIBUNALES, DEPTS] of var PROFESORES: miembros;  % Profesores en cada tribunal
array[TRIBUNALES] of var DIAS: dia_tribunal;          % Día asignado a cada tribunal
array[TFGS] of var TRIBUNALES: tfg_asignado;          % Tribunal asignado a cada TFG

% Restricciones de composición de tribunales
% Cada tribunal debe tener un profesor de cada departamento
constraint forall(t in TRIBUNALES) (
  miembros[t, DECSAI] in PROFESORES_DECSAI /\
  miembros[t, LSI] in PROFESORES_LSI /\
  miembros[t, ICAR] in PROFESORES_ICAR
);

% Un profesor solo puede estar en un tribunal
constraint forall(p in PROFESORES) (
  sum(t in TRIBUNALES, d in DEPTS) (bool2int(miembros[t, d] = p)) <= 1
);

% Restricciones de disponibilidad de profesores por día
% Profesor 1 solo disponible el martes
constraint forall(t in TRIBUNALES) (
  miembros[t, DECSAI] = 1 -> dia_tribunal[t] = MARTES
);

% Profesores 2 y 3 disponibles lunes, miércoles y viernes
constraint forall(t in TRIBUNALES) (
  (miembros[t, DECSAI] = 2 \/ miembros[t, DECSAI] = 3) -> 
  (dia_tribunal[t] = LUNES \/ dia_tribunal[t] = MIERCOLES \/ dia_tribunal[t] = VIERNES)
);

% Profesor 4 disponible miércoles y jueves
constraint forall(t in TRIBUNALES) (
  miembros[t, LSI] = 4 -> (dia_tribunal[t] = MIERCOLES \/ dia_tribunal[t] = JUEVES)
);

% Profesor 5 disponible lunes y jueves
constraint forall(t in TRIBUNALES) (
  miembros[t, LSI] = 5 -> (dia_tribunal[t] = LUNES \/ dia_tribunal[t] = JUEVES)
);

% Profesor 7 disponible solo viernes
constraint forall(t in TRIBUNALES) (
  miembros[t, ICAR] = 7 -> dia_tribunal[t] = VIERNES
);

% Profesor 8 disponible miércoles y jueves
constraint forall(t in TRIBUNALES) (
  miembros[t, ICAR] = 8 -> (dia_tribunal[t] = MIERCOLES \/ dia_tribunal[t] = JUEVES)
);

% Profesor 9 disponible solo jueves
constraint forall(t in TRIBUNALES) (
  miembros[t, ICAR] = 9 -> dia_tribunal[t] = JUEVES
);

% Restricciones de incompatibilidad director-tribunal
% Profesor 2 dirigió TFGs 1 y 10
constraint forall(t in TRIBUNALES) (
  miembros[t, DECSAI] = 2 -> (tfg_asignado[1] != t /\ tfg_asignado[10] != t)
);

% Profesor 3 dirigió TFG 8 y codirigió TFG 7 con profesor 8
constraint forall(t in TRIBUNALES) (
  miembros[t, DECSAI] = 3 -> (tfg_asignado[8] != t /\ tfg_asignado[7] != t)
);

constraint forall(t in TRIBUNALES) (
  miembros[t, ICAR] = 8 -> (tfg_asignado[7] != t /\ tfg_asignado[4] != t)
);

% Profesor 4 dirigió TFG 6 y codirigió TFG 4 con profesor 8
constraint forall(t in TRIBUNALES) (
  miembros[t, LSI] = 4 -> (tfg_asignado[6] != t /\ tfg_asignado[4] != t)
);

% Profesor 5 dirigió TFG 2
constraint forall(t in TRIBUNALES) (
  miembros[t, LSI] = 5 -> tfg_asignado[2] != t
);

% Profesores 6 y 7 codirigieron TFG 3
constraint forall(t in TRIBUNALES) (
  (miembros[t, LSI] = 6 \/ miembros[t, ICAR] = 7) -> tfg_asignado[3] != t
);

% Restricciones adicionales
% Máximo 5 TFGs por tribunal
constraint forall(t in TRIBUNALES) (
  sum(tfg in TFGS) (bool2int(tfg_asignado[tfg] = t)) <= 5
);

% Tribunales en días diferentes
constraint dia_tribunal[T1] != dia_tribunal[T2];

% Romper simetría entre tribunales - T1 se celebra antes que T2
constraint dia_tribunal[T1] < dia_tribunal[T2];

solve satisfy;

% Salida formateada
output [
  "Tribunal 1: ", show(miembros[T1, DECSAI]), "(DECSAI), ", 
  show(miembros[T1, LSI]), "(LSI), ",
  show(miembros[T1, ICAR]), "(ICAR) (Día: ", 
  if fix(dia_tribunal[T1]) = LUNES then "Lunes"
  elseif fix(dia_tribunal[T1]) = MARTES then "Martes"
  elseif fix(dia_tribunal[T1]) = MIERCOLES then "Miércoles"
  elseif fix(dia_tribunal[T1]) = JUEVES then "Jueves"
  else "Viernes" endif, ")\n",
  "TFGs a evaluar: ", join(", ", [ show(tfg) | tfg in TFGS where fix(tfg_asignado[tfg]) = T1 ]), "\n",
  "Tribunal 2: ", show(miembros[T2, DECSAI]), "(DECSAI), ", 
  show(miembros[T2, LSI]), "(LSI), ",
  show(miembros[T2, ICAR]), "(ICAR) (Día: ", 
  if fix(dia_tribunal[T2]) = LUNES then "Lunes"
  elseif fix(dia_tribunal[T2]) = MARTES then "Martes"
  elseif fix(dia_tribunal[T2]) = MIERCOLES then "Miércoles"
  elseif fix(dia_tribunal[T2]) = JUEVES then "Jueves"
  else "Viernes" endif, ")\n",
  "TFGs a evaluar: ", join(", ", [ show(tfg) | tfg in TFGS where fix(tfg_asignado[tfg]) = T2 ])
];