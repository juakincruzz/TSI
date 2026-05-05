% ============================================================
% FICHERO    : Ejercicio6.mzn
%
% TIPO       : CSP (solve satisfy)
%
% PARÁMETROS :
%   disponible[p, d] - Disponibilidad del profesor p el día d (bool).
%   sesion_fija[p]   - Sesión obligatoria del profesor p
%                      (1=mañana, 2=tarde, 0=sin restricción).
%
% VARIABLES DE DECISIÓN:
%   prof_decsai[t]  - Profesor de DECSAI en el tribunal t (1..4).
%   prof_lsi[t]     - Profesor de LSI en el tribunal t (5..7).
%   prof_icar[t]    - Profesor de ICAR en el tribunal t (8..10).
%   dia_trib[t]     - Día del tribunal t (1=Lun .. 5=Vie).
%   sesion_trib[t]  - Sesión del tribunal t (1=Mañana, 2=Tarde).
%   tribunal_tfg[k] - Tribunal asignado al TFG k (1..3).
%
% RESTRICCIONES PRINCIPALES:
%   · Cada profesor aparece en máximo 1 tribunal (prof 10: máx 2).
%   · Disponibilidad y sesión fija de cada profesor.
%   · Un director no evalúa su propio TFG ni co-director el suyo.
%   · Cada tribunal evalúa entre 4 y 6 TFGs.
%   · TFGs del mismo director van al mismo tribunal.
%   · Un profesor no puede tener dos tribunales el mismo día.
%   · Rotura de simetrías: prof_decsai[1] < [2] < [3].
% ============================================================

include "globals.mzn";

% ============================================================
% PARÁMETROS
% ============================================================
int: nProf    = 10;
int: nTFG     = 12;
int: nTrib    = 3;

set of int: Profesores = 1..nProf;
set of int: TFGs       = 1..nTFG;
set of int: Tribunales = 1..nTrib;

% Departamentos: DECSAI=1..4, LSI=5..7, ICAR=8..10
set of int: DECSAI = 1..4;
set of int: LSI    = 5..7;
set of int: ICAR   = 8..10;

% Días: 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes
% Sesiones: 1=Mañana, 2=Tarde
int: nDias    = 5;
int: nSesiones = 2;


% DISPONIBILIDAD de profesores [prof, dia] -> bool
array[Profesores, 1..nDias] of bool: disponible = 
    [| false, true,  false, false, false   % Prof 1: solo martes
     | true,  false, true,  false, true    % Prof 2: lunes, miércoles, viernes
     | true,  false, true,  false, false   % Prof 3: lunes, miércoles
     | false, false, true,  true,  false   % Prof 4: miércoles, jueves
     | true,  false, false, true,  false   % Prof 5: lunes, jueves
     | true,  true,  true,  true,  true    % Prof 6: todos
     | false, false, false, false, true    % Prof 7: viernes
     | false, false, true,  true,  false   % Prof 8: miércoles, jueves
     | false, false, false, true,  false   % Prof 9: solo jueves
     | true,  true,  false, false, true    % Prof 10: lunes, martes, viernes
    |];

% Restricción de sesión: solo mañana -> sesión=1; solo tarde -> sesión=2
% Prof 1,5,8: solo mañana; Prof 2,4,6: solo tarde
array[Profesores] of int: sesion_fija = 
    [1, 2, 0, 2, 1, 2, 0, 1, 0, 0];
% 0 = sin restricción de sesión

% ============================================================
% VARIABLES
% ============================================================
% Qué profesor ocupa el rol DECSAI, LSI e ICAR en cada tribunal
array[Tribunales] of var DECSAI: prof_decsai;
array[Tribunales] of var LSI:    prof_lsi;
array[Tribunales] of var ICAR:   prof_icar;

% Día y sesión de cada tribunal
array[Tribunales] of var 1..nDias:    dia_trib;
array[Tribunales] of var 1..nSesiones: sesion_trib;

% Asignación de TFGs a tribunales
array[TFGs] of var Tribunales: tribunal_tfg;

% ============================================================
% RESTRICCIONES DE COMPOSICIÓN DE TRIBUNALES
% ============================================================
% Cada tribunal tiene exactamente 1 prof de cada depto (ya garantizado por dominios)

% Prof 10 puede estar en hasta 2 tribunales; los demás en máximo 1
% Para cada prof 1..9, aparece en como mucho 1 tribunal
constraint forall(p in 1..9)(
    sum(t in Tribunales)(
        bool2int(prof_decsai[t] = p) +
        bool2int(prof_lsi[t] = p) +
        bool2int(prof_icar[t] = p)
    ) <= 1
);
% Prof 10: máximo 2 tribunales
constraint sum(t in Tribunales)(bool2int(prof_icar[t] = 10)) <= 2;

% ============================================================
% RESTRICCIONES DE DISPONIBILIDAD
% ============================================================
constraint forall(t in Tribunales)(
    disponible[prof_decsai[t], dia_trib[t]] /\
    disponible[prof_lsi[t],    dia_trib[t]] /\
    disponible[prof_icar[t],   dia_trib[t]]
);

% Restricciones de sesión fija
constraint forall(t in Tribunales)(
    (sesion_fija[prof_decsai[t]] != 0 -> sesion_trib[t] = sesion_fija[prof_decsai[t]]) /\
    (sesion_fija[prof_lsi[t]]    != 0 -> sesion_trib[t] = sesion_fija[prof_lsi[t]])    /\
    (sesion_fija[prof_icar[t]]   != 0 -> sesion_trib[t] = sesion_fija[prof_icar[t]])
);

% ============================================================
% RESTRICCIONES DE DIRECCIÓN DE TFGs
% ============================================================
% Un director no puede estar en el tribunal de su TFG
% Prof 2 -> TFG 1, 10
constraint forall(t in Tribunales)(
    (tribunal_tfg[1]  = t -> prof_decsai[t] != 2) /\
    (tribunal_tfg[10] = t -> prof_decsai[t] != 2)
);
% Prof 3 -> TFG 8, codir TFG 7 con prof 8
constraint forall(t in Tribunales)(
    (tribunal_tfg[8] = t -> prof_decsai[t] != 3) /\
    (tribunal_tfg[7] = t -> prof_decsai[t] != 3 /\ prof_icar[t] != 8)
);
% Prof 4 -> TFG 6, 12; codir TFG 4 con prof 8
constraint forall(t in Tribunales)(
    (tribunal_tfg[6]  = t -> prof_decsai[t] != 4) /\
    (tribunal_tfg[12] = t -> prof_decsai[t] != 4) /\
    (tribunal_tfg[4]  = t -> prof_decsai[t] != 4 /\ prof_icar[t] != 8)
);
% Prof 5 -> TFG 2
constraint forall(t in Tribunales)(
    tribunal_tfg[2] = t -> prof_lsi[t] != 5
);
% Prof 6 codir con prof 7 -> TFG 3, 5
constraint forall(t in Tribunales)(
    (tribunal_tfg[3] = t -> prof_lsi[t] != 6 /\ prof_lsi[t] != 7) /\
    (tribunal_tfg[5] = t -> prof_lsi[t] != 6 /\ prof_lsi[t] != 7)
);
% Prof 10 codir con prof 1 -> TFG 9, 11
constraint forall(t in Tribunales)(
    (tribunal_tfg[9]  = t -> prof_icar[t] != 10 /\ prof_decsai[t] != 1) /\
    (tribunal_tfg[11] = t -> prof_icar[t] != 10 /\ prof_decsai[t] != 1)
);

% ============================================================
% RESTRICCIONES DE TFGs POR TRIBUNAL
% ============================================================
% Cada tribunal evalúa entre 4 y 6 TFGs
constraint forall(t in Tribunales)(
    let { var int: n = sum(tfg in TFGs)(bool2int(tribunal_tfg[tfg] = t)) } in
    n >= 4 /\ n <= 6
);

% TFGs 6 y 12 (ambos de prof 4) deben ir al mismo tribunal
constraint tribunal_tfg[6] = tribunal_tfg[12];

% TFGs 1 y 10 (ambos de prof 2) deben ir al mismo tribunal
constraint tribunal_tfg[1] = tribunal_tfg[10];

% ============================================================
% UN MISMO PROFESOR NO PUEDE TENER DOS TRIBUNALES EL MISMO DÍA
% ============================================================
constraint forall(t1, t2 in Tribunales where t1 < t2)(
    dia_trib[t1] = dia_trib[t2] ->
        prof_decsai[t1] != prof_decsai[t2] /\
        prof_lsi[t1]    != prof_lsi[t2]    /\
        prof_icar[t1]   != prof_icar[t2]   /\
        % Prof 10 puede estar en 2 tribunales pero no el mismo día
        not (prof_icar[t1] = 10 /\ prof_icar[t2] = 10)
);


% ROTURA DE SIMETRÍAS
% Los tribunales son intercambiables entre sí -> fijamos orden
% por el número del profesor DECSAI (menor índice primero)
constraint prof_decsai[1] < prof_decsai[2];
constraint prof_decsai[2] < prof_decsai[3];


% RESOLVER
solve satisfy;


% OUTPUT
array[1..nDias] of string: nombreDia = 
    ["Lunes", "Martes", "Miercoles", "Jueves", "Viernes"];
array[1..nSesiones] of string: nombreSesion = ["Mañana", "Tarde"];

output [
    let { array[int] of int: lista = 
        [tfg | tfg in TFGs where fix(tribunal_tfg[tfg]) = t] } in
    "Tribunal " ++ show(t) ++ "\n" ++
    show(fix(prof_decsai[t])) ++ "/DECSAI, " ++
    show(fix(prof_lsi[t]))    ++ "/LSI, " ++
    show(fix(prof_icar[t]))   ++ "/ICAR\n" ++
    "Dia: " ++ nombreDia[fix(dia_trib[t])] ++
    ", Sesion: " ++ nombreSesion[fix(sesion_trib[t])] ++ "\n" ++
    "TFGs a evaluar: " ++
    concat([show(lista[i]) ++ if i < length(lista) then "," else "" endif
            | i in 1..length(lista)]) ++ "\n\n"
    | t in Tribunales
];