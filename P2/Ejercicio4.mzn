include "globals.mzn";

int: X;
set of int: DOMINIO = 1..X-1;
array[DOMINIO] of var bool: seleccionado;

% Restricción principal
constraint sum(y in DOMINIO)(y*y * bool2int(seleccionado[y])) = X*X;

% Variable para la cardinalidad
var int: cardinalidad = sum(seleccionado);

% Restricciones de poda adicionales
% (puedes añadir otras según tu análisis)
constraint cardinalidad <= X-1; % No puede haber más de X-1 elementos

% Maximizar cardinalidad
solve maximize cardinalidad;

output [
    "x = ", show(X), "\n",
    "S = {", 
    join(", ", [show(y) | y in DOMINIO where fix(seleccionado[y])]), 
    "}\nCardinalidad = ", show(cardinalidad),
    "\nSuma de cuadrados = ", show(X*X)
];




