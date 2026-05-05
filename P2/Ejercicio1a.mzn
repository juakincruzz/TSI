% ============================================================
% FICHERO    : Ejercicio1a.mzn
%
% TIPO       : CSP (solve satisfy)
%
% PARÁMETROS :
%   V             - Volumen total de la bebida en ml.
%                   Para probar lo que hago es descomentar la línea deseada (200/1000/5000/10000).
%   nIng          - Número de ingredientes (= 6).
%   azucar[i]     - Azúcar (g) por cada 100 ml del ingrediente i.
%   sodio[i]      - Sodio (mg) por cada 100 ml del ingrediente i.
%   cafeina[i]    - Cafeína (mg) por cada 100 ml del ingrediente i.
%   coste_cent[i] - Coste (céntimos) por cada 100 ml del ingrediente i.
%
% VARIABLES DE DECISIÓN:
%   u[i] - Unidades de 10 ml del ingrediente i (dominio 0..V/10).
%           El volumen real en ml es u[i] * 10.
%
% RESTRICCIONES PRINCIPALES:
%   · sum(u[i]) = V/10  (volumen total exacto)
%   · Azúcar  en [70, 85]   g  por 1000 ml
%   · Sodio   en [400, 475] mg por 1000 ml
%   · Cafeína en [60, 90]   mg por 1000 ml
%   · Aroma cítrico: entre 3% y 20% del volumen total
%   · Extracto energético: máximo 40% del volumen total
%   · Exactamente uno de los dos jarabes (A o B)
%   · Jarabe A, si se usa: mínimo 5% del volumen total
%   · Concentrado salino, si se usa: mínimo 3% del volumen total
%   · Como mucho 4 ingredientes distintos
% ============================================================

include "globals.mzn";

% ============================================================
% PARÁMETRO DE ENTRADA: volumen total buscado (en ml)
% Descomentar una sola línea para probar cada volumen:
% ============================================================
% int: V = 200;
% int: V = 1000;
% int: V = 5000;
 int: V = 10000;

% ============================================================
% DATOS DEL PROBLEMA
% ============================================================
% Número de ingredientes
int: nIng = 6;
set of int: ING = 1..nIng;

% Indices de ingredientes para legibilidad
int: AGUA = 1;
int: JARABE_A = 2;
int: JARABE_B = 3;
int: SALINO = 4;
int: EXTRACTO = 5;
int: AROMA = 6;

% Datos nutricionales por cada 100 ml de ingrediente

% Azúcar en g por 100 ml
array[ING] of int: azucar = [0, 18, 12, 0, 4, 2];

% Sodio en mg por 100 ml
array[ING] of int: sodio = [0, 40, 20, 180, 10, 5];

% Cafeína en mg por 100 ml
array[ING] of int: cafeina = [0, 0, 0, 0, 30, 0];

% Coste en céntimos por 100 ml (euros * 100 para evitar floats)
array[ING] of int: coste_cent = [2, 12, 10, 8, 20, 6];

% Nombres de ingredientes para la salida
array[ING] of string: nombre = [
  "Agua base", "Jarabe A", "Jarabe B",
  "Concentrado salino", "Extracto energetico", "Aroma citrico"
];

% ============================================================
% VARIABLES DE DECISIÓN
% ============================================================
% u[i] = número de unidades de 10 ml del ingrediente i
% Así, ml reales = u[i] * 10, y siempre son múltiplos de 10
int: maxUnidades = V div 10;
array[ING] of var 0..maxUnidades: u;

% ============================================================
% RESTRICCIONES
% ============================================================

% Volumen total = V ml
% sum(u[i]) * 10 = V  =>  sum(u[i]) = V / 10
constraint sum(i in ING)(u[i]) = maxUnidades;

% Restricciones nutricionales
% Contribución real de azúcar = sum(azucar[i] * u[i]) / 10 gramos
% Límites por 1000 ml: [70, 85] g => para V ml: [70*V/1000, 85*V/1000]
% Para evitar floats, multiplicamos todo por 1000:
%   sum(azucar[i] * u[i]) * 100 >= 70 * V
%   sum(azucar[i] * u[i]) * 100 <= 85 * V

% Azúcar: entre 70 y 85 g por cada 1000 ml
constraint sum(i in ING)(azucar[i] * u[i]) * 100 >= 70 * V;
constraint sum(i in ING)(azucar[i] * u[i]) * 100 <= 85 * V;

% Sodio: entre 400 y 475 mg por cada 1000 ml
constraint sum(i in ING)(sodio[i] * u[i]) * 100 >= 400 * V;
constraint sum(i in ING)(sodio[i] * u[i]) * 100 <= 475 * V;

% Cafeína: entre 60 y 90 mg por cada 1000 ml
constraint sum(i in ING)(cafeina[i] * u[i]) * 100 >= 60 * V;
constraint sum(i in ING)(cafeina[i] * u[i]) * 100 <= 90 * V;

% Restricciones adicionales:

% Aroma cítrico: obligatorio entre 3% y 20% del volumen total
% u[AROMA] * 10 >= 0.03 * V  =>  u[AROMA] * 1000 >= 3 * V
constraint u[AROMA] * 1000 >= 3 * V;
constraint u[AROMA] * 1000 <= 20 * V;

% Extracto energético: no puede superar el 40% del volumen total
constraint u[EXTRACTO] * 1000 <= 40 * V;

% Exactamente uno de los dos jarabes (A o B), no ambos
constraint (u[JARABE_A] > 0) + (u[JARABE_B] > 0) = 1;

% Si se usa Jarabe A, debe emplearse al menos en el 5% del volumen total
constraint (u[JARABE_A] > 0) -> (u[JARABE_A] * 1000 >= 5 * V);

% Si se usa concentrado salino, debe emplearse al menos en el 3% del volumen
constraint (u[SALINO] > 0) -> (u[SALINO] * 1000 >= 3 * V);

% Como mucho pueden usarse 4 ingredientes distintos
constraint sum(i in ING)(bool2int(u[i] > 0)) <= 4;


% RESOLVER: CSP (satisfy)
solve satisfy;


% OUTPUT
output [
  "Cantidades usadas:\n",
  "------------------\n"
] ++
[ "\(nombre[i]): \(u[i] * 10) ml\n" | i in ING ] ++
[
  "\nCoste total = \(sum(i in ING)(coste_cent[i] * u[i]) div 10).\(sum(i in ING)(coste_cent[i] * u[i]) mod 10) centimos\n",
  "Azucar total = \(sum(i in ING)(azucar[i] * u[i]) div 10).\(sum(i in ING)(azucar[i] * u[i]) mod 10) g\n",
  "Sodio total = \(sum(i in ING)(sodio[i] * u[i]) div 10).\(sum(i in ING)(sodio[i] * u[i]) mod 10) mg\n",
  "Cafeina total = \(sum(i in ING)(cafeina[i] * u[i]) div 10).\(sum(i in ING)(cafeina[i] * u[i]) mod 10) mg\n"
];
