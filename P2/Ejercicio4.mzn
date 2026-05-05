% ============================================================
% FICHERO    : Ejercicio4.mzn
%
% TIPO       : COP (solve minimize suma_total)
%
% PARÁMETROS :
%   N    - Tamaño de la matriz. Descomentar una línea (3/4/5/6/7).
%   MAXV - Valor máximo permitido en las celdas (= 100).
%
% VARIABLES DE DECISIÓN:
%   matriz[i,j]   - Valor en la celda (i,j); pertenece al conjunto Valores.
%   suma_comun    - Suma común de filas, columnas y diagonales.
%   suma_total    - Suma de todos los elementos (= N * suma_comun).
%   num_distintos - Número de valores distintos en la matriz.
%
% RESTRICCIONES PRINCIPALES:
%   · Cada fila, columna y diagonal suman suma_comun.
%   · nvalue(num_distintos, matriz) con num_distintos >= ceil(N²/2).
%   · Rotura D4: matriz[1,1] < las otras 3 esquinas;
%                matriz[1,N] < matriz[N,1].
% ============================================================

include "globals.mzn";

% ============================================================
% PARÁMETRO DE ENTRADA: tamaño N de la matriz
% El profesor debe descomentar solo uno y comentar los demás.
% ============================================================
% int: N = 3;
% int: N = 4;
% int: N = 5;
% int: N = 6;
 int: N = 7;

int: MAXV = 100;

set of int: Rango = 1..N;

% ============================================================
% DOMINIO DE VALORES VÁLIDOS
% Si N es par -> solo valores pares (2, 4, 6, ..., 100)
% Si N es impar -> solo valores impares (1, 3, 5, ..., 99)
% Integramos la restricción par/impar DIRECTAMENTE en el dominio
% en lugar de aplicarla como restricción externa. Esto es más
% eficiente porque reduce el espacio de búsqueda desde el principio.
% ============================================================
int: inicio = if N mod 2 = 0 then 2 else 1 endif;
set of int: Valores = {inicio + 2*k | k in 0..(MAXV - inicio) div 2};

% ============================================================
% VARIABLES DE DECISIÓN
% ============================================================
% Cada celda toma un valor del conjunto Valores (pares o impares)
array[Rango, Rango] of var Valores: matriz;

% Suma común de filas, columnas y diagonales
var int: suma_comun;

% Suma total de todos los elementos (= suma_comun * N)
var int: suma_total;


% RESTRICCIONES
% Suma común por filas
constraint forall(i in Rango)(
    sum(j in Rango)(matriz[i, j]) = suma_comun
);

% Suma común por columnas
constraint forall(j in Rango)(
    sum(i in Rango)(matriz[i, j]) = suma_comun
);

% Suma común diagonal principal
constraint sum(i in Rango)(matriz[i, i]) = suma_comun;

% Suma común diagonal secundaria
constraint sum(i in Rango)(matriz[i, N - i + 1]) = suma_comun;

% Relación entre suma total y suma común
constraint suma_total = sum(i in Rango, j in Rango)(matriz[i, j]) ;

% Al menos la mitad de los valores deben ser distintos.
% ceil(N²/2) se calcula como (N*N + 1) div 2 para evitar floats.
int: min_distintos = (N * N + 1) div 2;
var min_distintos..(N*N): num_distintos;
constraint nvalue(num_distintos, matriz);

% ============================================================
% ROTURA DE SIMETRÍAS (grupo diédrico D4: 4 rotaciones + 4 reflexiones)
% ============================================================
% Un cuadrado tiene 8 transformaciones simétricas equivalentes.
% Para eliminarlas, fijamos un representante canónico imponiendo
% que la esquina superior izquierda sea estrictamente la menor
% de las 4 esquinas. Esto descarta 4 de las 8 simetrías (las rotaciones).
constraint matriz[1, 1] < matriz[1, N];   % sup-izq < sup-der
constraint matriz[1, 1] < matriz[N, 1];   % sup-izq < inf-izq
constraint matriz[1, 1] < matriz[N, N];   % sup-izq < inf-der

% Para eliminar las 4 reflexiones restantes, forzamos que la esquina
% superior derecha sea estrictamente menor que la inferior izquierda.
% Esto rompe la simetría de la reflexión sobre la diagonal principal.
constraint matriz[1, N] < matriz[N, 1];


% FUNCIÓN OBJETIVO: minimizar la suma total
solve minimize suma_total;


% OUTPUT
output [
    "N = " ++ show(N) ++ "\n" ++
    "MAXV = " ++ show(MAXV) ++ "\n" ++
    "Minimo de valores distintos exigido = " ++ show(min_distintos) ++ "\n" ++
    "Numero de valores distintos = " ++ show(num_distintos) ++ "\n" ++
    "Suma comun de filas/columnas/diagonales = " ++ show(suma_comun) ++ "\n" ++
    "Suma total minimizada = " ++ show(suma_total) ++ "\n\n" ++
    "Matriz:\n"
] ++ [
    show(matriz[i, j]) ++ if j = N then "\n" else " " endif
    | i in Rango, j in Rango
];