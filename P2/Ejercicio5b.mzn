% ============================================================
% FICHERO    : Ejercicio5b.mzn
%
% TIPO       : CSP (solve satisfy, makespan fijado a 29)
%
% PARÁMETROS :
%   (Idénticos al Ejercicio 5a)
%
% VARIABLES DE DECISIÓN:
%   (Idénticas al Ejercicio 5a)
%
% DIFERENCIA RESPECTO A 5a:
%    solve minimize makespan  pasa a constraint makespan = 29 + solve satisfy
% ============================================================

include "globals.mzn";

% ============================================================
% ENUMERADOS Y DATOS
% ============================================================
enum Tarea = {A, B, C, D, E, F, G, H, I, J, K};
enum Trabajador = {Marty, Doc};

% Duración base según trabajador: [Marty, Doc]
array[Tarea, 1..2] of int: durBase = 
    [| 10, 16   % A
     |  1,  2   % B
     |  5,  4   % C
     | 15,  5   % D
     | 12,  4   % E
     | 13,  5   % F
     |  7,  6   % G
     |  4,  3   % H
     |  2,  2   % I
     |  4,  2   % J
     |  4,  3   % K
    |];

% Predecesoras: list of (tarea, predecesora)
% Se codifican como restricciones directamente

% ============================================================
% VARIABLES
% ============================================================
array[Tarea] of var Trabajador: asignado;
array[Tarea] of var bool: biff;
array[Tarea] of var bool: george;
array[Tarea] of var 1..200: inicio;

% Duración real de cada tarea
array[Tarea] of var 1..16: dur;

% Makespan: día en que termina la última tarea
var int: makespan = max(t in Tarea)(inicio[t] + dur[t] - 1);

% ============================================================
% DURACIÓN REAL (base - reducción de Biff - reducción de George)
% ============================================================
constraint forall(t in Tarea)(
    dur[t] = (if asignado[t] = Marty then durBase[t,1] else durBase[t,2] endif)
           - (if biff[t] then (if t = A then 3 elseif t = H then 2 else 1 endif) else 0 endif)
           - (if george[t] then 1 else 0 endif)
);

% Duración siempre positiva
constraint forall(t in Tarea)(dur[t] >= 1);

% ============================================================
% RESTRICCIONES DE BIFF
% ============================================================
% 1. Biff solo apoya si la duración del trabajador asignado >= 3 días
constraint forall(t in Tarea)(
    biff[t] = true ->
        (if asignado[t] = Marty then durBase[t,1] else durBase[t,2] endif) >= 3
);

% 2. Biff apoya como máximo en 4 tareas
constraint sum(t in Tarea)(bool2int(biff[t])) <= 4;

% 3. Biff no puede apoyar en tareas simultáneas
constraint forall(t1, t2 in Tarea where t1 < t2)(
    biff[t1] /\ biff[t2] ->
        inicio[t1] + dur[t1] - 1 < inicio[t2] \/
        inicio[t2] + dur[t2] - 1 < inicio[t1]
);

% 4. Biff no puede ayudar en D ni en F
constraint biff[D] = false;
constraint biff[F] = false;

% 5. Biff no apoya en más de 2 tareas de Marty ni más de 2 de Doc
constraint sum(t in Tarea)(bool2int(biff[t] /\ asignado[t] = Marty)) <= 2;
constraint sum(t in Tarea)(bool2int(biff[t] /\ asignado[t] = Doc))   <= 2;

% ============================================================
% RESTRICCIONES DE GEORGE
% ============================================================
% 1. George solo ayuda a Marty
constraint forall(t in Tarea)(george[t] = true -> asignado[t] = Marty);

% 2. Duración original de Marty >= 2 días
constraint forall(t in Tarea)(
    george[t] = true -> durBase[t,1] >= 2
);

% 3. George no puede coincidir temporalmente con otra tarea que apoye
constraint forall(t1, t2 in Tarea where t1 < t2)(
    george[t1] /\ george[t2] ->
        inicio[t1] + dur[t1] - 1 < inicio[t2] \/
        inicio[t2] + dur[t2] - 1 < inicio[t1]
);

% 4. George y Biff no pueden coincidir en la misma tarea
constraint forall(t in Tarea)(not (biff[t] /\ george[t]));



% ============================================================
% RESTRICCIONES DE MARTY Y DOC (no simultaneidad)
% ============================================================
constraint forall(t1, t2 in Tarea where t1 < t2)(
    asignado[t1] = asignado[t2] ->
        inicio[t1] + dur[t1] - 1 < inicio[t2] \/
        inicio[t2] + dur[t2] - 1 < inicio[t1]
);

% ============================================================
% TAREA A empieza en día 1
% ============================================================
constraint inicio[A] = 1;


% PRECEDENCIAS
constraint inicio[B] >= inicio[A] + dur[A];
constraint inicio[C] >= inicio[B] + dur[B];
constraint inicio[D] >= inicio[A] + dur[A];
constraint inicio[D] >= inicio[C] + dur[C];
constraint inicio[E] >= inicio[C] + dur[C];
constraint inicio[F] >= inicio[C] + dur[C];
constraint inicio[F] >= inicio[D] + dur[D];
constraint inicio[F] >= inicio[E] + dur[E];
constraint inicio[G] >= inicio[C] + dur[C];
constraint inicio[G] >= inicio[D] + dur[D];
constraint inicio[H] >= inicio[E] + dur[E];
constraint inicio[H] >= inicio[G] + dur[G];
constraint inicio[I] >= inicio[F] + dur[F];
constraint inicio[I] >= inicio[H] + dur[H];
constraint inicio[J] >= inicio[G] + dur[G];
constraint inicio[J] >= inicio[I] + dur[I];
constraint inicio[K] >= inicio[H] + dur[H];
constraint inicio[K] >= inicio[I] + dur[I];


% OBJETIVO
% Cambio solve minimize makespan por:
constraint makespan = 29 ;
solve satisfy ;


% OUTPUT
array[Tarea] of string: nombreTarea = 
    ["Montar chasis", "Instalar ruedas", "Cableado electrico",
     "Motor de fusion nuclear", "Tablero de control",
     "Instalacion condensador de fluzo", "Ajuste aerodinamico",
     "Instalacion puertas ala gaviota", "Panel de tiempo",
     "Implementacion algoritmos busqueda", "Prueba de plutonio"];

output [
    show(t) ++ " - " ++ nombreTarea[t] ++
    " inicio dia " ++ show(fix(inicio[t])) ++
    " fin dia "   ++ show(fix(inicio[t]) + fix(dur[t]) - 1) ++
    ". Asignada a " ++ (if fix(asignado[t]) = Marty then "Marty" else "Doc" endif) ++
    (if fix(biff[t])   then " con apoyo de Biff"   else "" endif) ++
    (if fix(george[t]) then " con apoyo de George" else "" endif) ++
    "\n"
    | t in Tarea
];