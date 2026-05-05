% ============================================================
% FICHERO    : Ejercicio7.mzn
%
% TIPO       : CSP (solve satisfy)
%
% PARÁMETROS :
%   equipos[p, 1..2] - Local y visitante del partido p.
%
% VARIABLES DE DECISIÓN:
%   dia[p]    - Día del partido p (1..7, distinto de 4).
%   sesion[p] - Sesión del partido p (Tarde / Noche / PrimeTime).
%   btb[e, d] - true si el equipo e tiene back-to-back en días d y d+1.
%
% RESTRICCIONES PRINCIPALES:
%   · Día 4 prohibido; día 7 sin PrimeTime.
%   · LAL, BOS, GSW no pueden jugar en sesión Tarde.
%   · Un equipo no juega dos veces el mismo día.
%   · Máx 2 partidos por franja y día; máx 4 partidos por día.
%   · Al menos 3 partidos en PrimeTime.
%   · Partidos fijos: LAL-GSW (día 2, PT), BOS-LAL (día 6, PT), MIL-BOS (PT).
%   · Restricciones de pabellón (días prohibidos por partido).
%   · Días 5 y 6 con al menos 3 partidos cada uno.
%   · BOS y GSW sin back-to-backs; cada equipo máx 1 back-to-back.
%   · Total back-to-backs (por equipo) <= 2.
%   · Equipo que juega PrimeTime no puede jugar Tarde al día siguiente.
%   · Tarde > Noche > PrimeTime en número total de partidos.
% ============================================================

include "globals.mzn";

% ============================================================
% EQUIPOS Y PARTIDOS
% ============================================================
enum Equipo = {BOS, NYK, PHI, MIA, MIL, LAL, GSW, PHX, DEN, DAL};
enum Sesion = {Tarde, Noche, PrimeTime};

int: nPartidos = 12;
set of int: Partidos = 1..nPartidos;

% Local, visitante de cada partido
array[Partidos, 1..2] of Equipo: equipos = 
    [| BOS, NYK   %  1 Celtics vs Knicks
     | MIA, PHI   %  2 Heat vs Sixers
     | MIL, BOS   %  3 Bucks vs Celtics
     | LAL, GSW   %  4 Lakers vs Warriors
     | PHX, DEN   %  5 Suns vs Nuggets
     | DAL, LAL   %  6 Mavericks vs Lakers
     | GSW, PHX   %  7 Warriors vs Suns
     | NYK, MIA   %  8 Knicks vs Heat
     | PHI, MIL   %  9 Sixers vs Bucks
     | DEN, DAL   % 10 Nuggets vs Mavericks
     | BOS, LAL   % 11 Celtics vs Lakers
     | MIA, GSW   % 12 Heat vs Warriors
    |];

% ============================================================
% VARIABLES
% ============================================================
array[Partidos] of var 1..7: dia;
array[Partidos] of var Sesion: sesion;

% ============================================================
% RESTRICCIONES BÁSICAS
% ============================================================
% Día 4: no hay partidos (festivo)
constraint forall(p in Partidos)(dia[p] != 4);

% Día 7: no hay Prime Time
constraint forall(p in Partidos)(dia[p] = 7 -> sesion[p] != PrimeTime);

% LAL, BOS, GSW no pueden jugar en Tarde
constraint forall(p in Partidos)(
    (equipos[p,1] = LAL \/ equipos[p,2] = LAL \/
     equipos[p,1] = BOS \/ equipos[p,2] = BOS \/
     equipos[p,1] = GSW \/ equipos[p,2] = GSW)
    -> sesion[p] != Tarde
);

% Un equipo no juega más de un partido el mismo día
constraint forall(p1, p2 in Partidos where p1 < p2)(
    (equipos[p1,1] = equipos[p2,1] \/ equipos[p1,1] = equipos[p2,2] \/
     equipos[p1,2] = equipos[p2,1] \/ equipos[p1,2] = equipos[p2,2])
    -> dia[p1] != dia[p2]
);

% En una misma franja: máximo 2 partidos
constraint forall(d in 1..7, s in Sesion)(
    sum(p in Partidos)(bool2int(dia[p] = d /\ sesion[p] = s)) <= 2
);

% En un mismo día: máximo 4 partidos
constraint forall(d in 1..7)(
    sum(p in Partidos)(bool2int(dia[p] = d)) <= 4
);

% Al menos 3 partidos en Prime Time
constraint sum(p in Partidos)(bool2int(sesion[p] = PrimeTime)) >= 3;

% Partidos fijos
constraint dia[4] = 2 /\ sesion[4] = PrimeTime;   % LAL vs GSW: día 2 Prime Time
constraint dia[11] = 6 /\ sesion[11] = PrimeTime;  % BOS vs LAL: día 6 Prime Time
constraint sesion[3] = PrimeTime;                   % MIL vs BOS: Prime Time

% Restricciones de pabellón
constraint dia[8] != 5;   % NYK vs MIA no puede ser día 5
constraint dia[5] != 2;   % PHX vs DEN no puede ser día 2
constraint dia[3] != 1;   % MIL vs BOS no puede ser día 1
constraint dia[10] != 6;  % DEN vs DAL no puede ser día 6

% Días 5 y 6 deben tener al menos 3 partidos cada uno
constraint sum(p in Partidos)(bool2int(dia[p] = 5)) >= 3;
constraint sum(p in Partidos)(bool2int(dia[p] = 6)) >= 3;

% ============================================================
% BACK2BACK
% ============================================================
% Un equipo tiene back2back si juega en días consecutivos
% Cada equipo: máximo 1 back-to-back
% BOS y GSW: ningún back-to-back
% Total back-to-backs <= 2

% Función auxiliar: ¿equipo e juega el día d?
predicate juega(Equipo: e, var int: d) =
    exists(p in Partidos)(dia[p] = d /\ (equipos[p,1] = e \/ equipos[p,2] = e));

% Back-to-back de un equipo: juega en días d y d+1
% Definimos variable para cada equipo y par de días consecutivos
array[Equipo, 1..6] of var bool: btb;
constraint forall(e in Equipo, d in 1..6)(
    btb[e, d] = (juega(e, d) /\ juega(e, d+1))
);

% BOS y GSW: ningún back-to-back
constraint forall(d in 1..6)(btb[BOS, d] = false);
constraint forall(d in 1..6)(btb[GSW, d] = false);

% Cada equipo: máximo 1 back-to-back
constraint forall(e in Equipo)(
    sum(d in 1..6)(bool2int(btb[e, d])) <= 1
);

% Total back-to-backs globales <= 2
% Un back-to-back cuenta por equipo: si 2 equipos tienen btb el mismo par de días
% contamos por equipos individuales (no por partido)
constraint sum(e in Equipo, d in 1..6)(bool2int(btb[e, d])) <= 2;

% Si un equipo juega en Prime Time, no puede jugar al día siguiente en Tarde
constraint forall(e in Equipo, p1 in Partidos, p2 in Partidos where p1 != p2)(
    (equipos[p1,1] = e \/ equipos[p1,2] = e) /\
    (equipos[p2,1] = e \/ equipos[p2,2] = e) /\
    sesion[p1] = PrimeTime /\ sesion[p2] = Tarde
    -> dia[p2] != dia[p1] + 1
);

% Tarde > Noche > Prime Time (en número de partidos)
constraint sum(p in Partidos)(bool2int(sesion[p] = Tarde)) >
           sum(p in Partidos)(bool2int(sesion[p] = Noche));

constraint sum(p in Partidos)(bool2int(sesion[p] = Noche)) >
           sum(p in Partidos)(bool2int(sesion[p] = PrimeTime));


% SOLVE
solve satisfy;


% OUTPUT
array[Equipo] of string: siglas = 
    ["BOS","NYK","PHI","MIA","MIL","LAL","GSW","PHX","DEN","DAL"];
array[Sesion] of string: nomSesion = ["Tarde","Noche","Prime Time"];
array[1..7] of string: nomDia = 
    ["Dia 1","Dia 2","Dia 3","Dia 4","Dia 5","Dia 6","Dia 7"];

% Ancho fijo de cada celda de día
int: W = 16;

function string: pad(string: s) =
    s ++ concat([" " | _ in 1..W - string_length(s)]);

output [
    "===========================\n" ++
    "Back-to-backs totales = " ++
    show(sum(e in Equipo, d in 1..6)(bool2int(fix(btb[e,d])))) ++ "\n\n"
] ++
% Cabecera
[ "+-----------+" ++ concat(["------------------+" | _ in 1..7]) ++ "\n" ] ++
[ "| Sesion    | Dia 1          | Dia 2          | Dia 3          | Dia 4          | Dia 5          | Dia 6          | Dia 7          |\n" ] ++
[ "+-----------+" ++ concat(["------------------+" | _ in 1..7]) ++ "\n" ] ++
% Filas por sesión
[
    let {
        array[1..7] of string: celdas = [
            if d = 4 then "- "
            else
                let { array[int] of int: pp =
                    [p | p in Partidos where fix(dia[p]) = d /\ fix(sesion[p]) = s] } in
                if length(pp) = 0 then "- "
                else concat([siglas[equipos[pp[i],1]] ++ "-" ++
                              siglas[equipos[pp[i],2]] ++ " "
                             | i in 1..length(pp)])
                endif
            endif
            | d in 1..7
        ]
    } in
    "| " ++ nomSesion[s] ++
    concat([" | " ++ pad(celdas[d]) | d in 1..7]) ++ " |\n" ++
    "+-----------+" ++ concat(["------------------+" | _ in 1..7]) ++ "\n"
    | s in Sesion
] ++
["\nLeyenda:\nBOS=Celtics, NYK=Knicks, PHI=Sixers, MIA=Heat, MIL=Bucks,\nLAL=Lakers, GSW=Warriors, PHX=Suns, DEN=Nuggets, DAL=Mavericks\n"];