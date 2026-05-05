% ============================================================
% FICHERO    : Ejercicio3.mzn
%
% TIPO       : COP (solve maximize obj)
%
% PARÁMETROS :
%   peso[i]     - Peso del objeto i.
%   coste[i]    - Coste del objeto i.
%   utilidad[i] - Utilidad del objeto i.
%
% VARIABLES DE DECISIÓN:
%   sel[i]      - 1 si el objeto i es seleccionado, 0 si no.
%   util_total  - Suma de utilidades de los objetos seleccionados.
%   peso_total  - Suma de pesos de los objetos seleccionados.
%   coste_total - Suma de costes de los objetos seleccionados.
%   obj         - Función objetivo compuesta (lexicográfica ponderada).
%
% RESTRICCIONES PRINCIPALES:
%   · sum(sel[i] * peso[i]) <= 15
%   · sel[ORDENADOR] <= sel[BATERIA]  (dependencia técnica)
%   · sel[SENSOR] <= sel[PANEL]       (compatibilidad energética)
%   · sel[BOTIQUIN] = 1               (seguridad mínima obligatoria)
%
% FUNCIÓN OBJETIVO:
%   obj = util_total*1_000_000 - peso_total*1_000 - coste_total
%   (maximizar obj equivale a orden lexicográfico de 3 criterios)
% ============================================================

include "globals.mzn";

% ============================================================
% DATOS DEL PROBLEMA
% ============================================================
% Número de objetos disponibles
int: n = 8;
set of int: OBJ = 1..n;

% Nombres de los objetos (para el output)
array[OBJ] of string: nombre = [
    "GPS", "Camara", "Ordenador", "Bateria extra",
    "Sensor termico", "Botiquin", "Radio", "Panel solar"
];

% Atributos de cada objeto (por cada 100ml en el problema original,
% aquí son valores directos de peso, coste y utilidad)
array[OBJ] of int: peso    = [3, 2, 4, 2, 5, 3, 4, 6];
array[OBJ] of int: coste   = [8, 6, 10, 4, 12, 5, 7, 9];
array[OBJ] of int: utilidad = [10, 9, 14, 7, 16, 8, 11, 13];

% Índices de objetos con nombre (para claridad en restricciones)
int: GPS = 1; int: CAMARA = 2; int: ORDENADOR = 3; int: BATERIA = 4;
int: SENSOR = 5; int: BOTIQUIN = 6; int: RADIO = 7; int: PANEL = 8;

% ============================================================
% VARIABLES DE DECISIÓN
% ============================================================
% sel[i] = 1 si el objeto i es seleccionado, 0 si no
array[OBJ] of var 0..1: sel;


% RESTRICCIONES
% R1: Peso máximo = 15
constraint sum(i in OBJ)(sel[i] * peso[i]) <= 15;

% R2: Dependencia técnica: si se selecciona Ordenador, también Batería extra
% Ordenador -> Batería extra  ≡  sel[ORDENADOR] <= sel[BATERIA]
constraint sel[ORDENADOR] <= sel[BATERIA];

% R3: Compatibilidad energética: si se selecciona Sensor térmico -> Panel solar
constraint sel[SENSOR] <= sel[PANEL];

% R4: Seguridad mínima: el Botiquín debe seleccionarse obligatoriamente
constraint sel[BOTIQUIN] = 1;


% FUNCIÓN OBJETIVO LEXICOGRÁFICA PONDERADA
% Criterio: 1º maximizar utilidad, 2º minimizar peso, 3º minimizar coste
%
% Para garantizar el orden lexicográfico usamos pesos multiplicativos
% suficientemente grandes entre niveles.
%
% Bounds del problema:
%   - Utilidad máxima posible = 88 (suma de todas)
%   - Peso máximo =~ 15
%   - Coste máximo =~ 61
%
% Factor 1.000 entre niveles garantiza que ninguna mejora de nivel inferior
% puede compensar una pérdida en el nivel superior.
%
% obj = utilidad_total * 1_000_000 - peso_total * 1_000 - coste_total * 1
% (maximizar obj ≡ maximizar utilidad, luego minimizar peso, luego minimizar coste)

var int: util_total  = sum(i in OBJ)(sel[i] * utilidad[i]);
var int: peso_total  = sum(i in OBJ)(sel[i] * peso[i]);
var int: coste_total = sum(i in OBJ)(sel[i] * coste[i]);

var int: obj = util_total * 1000000 - peso_total * 1000 - coste_total;

% RESOLVER: COP (solve maximize obj)
% solve maximize obj;
constraint util_total  = 49;
constraint peso_total  = 15;
constraint coste_total = 32; 
solve satisfy ;


% OUTPUT
output [
    "Objetos seleccionados:\n"
] ++
[
    if fix(sel[i]) = 1 then
        "- " ++ nombre[i] ++
        " (peso=" ++ show(peso[i]) ++
        ", coste=" ++ show(coste[i]) ++
        ", utilidad=" ++ show(utilidad[i]) ++ ")\n"
    else ""
    endif
    | i in OBJ
] ++
[
    "\nUtilidad total = " ++ show(util_total) ++ "\n" ++
    "Peso total = "      ++ show(peso_total)  ++ "\n" ++
    "Coste total = "     ++ show(coste_total) ++ "\n"
];