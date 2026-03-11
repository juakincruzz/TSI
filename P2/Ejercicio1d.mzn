
%  Busca UNA solución que use el mínimo nº de monedas

int: importe_cts;                       
                                       

%  Denominaciones en orden DE MAYOR A MENOR 
array[1..9] of int: valor =
      [ 50000, 20000, 10000,   500,  250,  50,  25,   5,   1 ];
      %   5 U     2 U     1 U   5c   25c  50c 25c 5c 1c  (en céntimos)

%  Variables 
array[1..9] of var 0..importe_cts div valor[9]: n;      % nº de monedas
var int: numMonedas = sum(i in 1..9)(n[i]);

% Restricciones 
constraint sum(i in 1..9)(valor[i] * n[i]) = importe_cts;

% Bound tightening: en una solución óptima no puede haber
% más monedas de un valor que (valor_superior/valor_actual − 1)
constraint
  forall(i in 1..8)(
     n[i] < valor[i] div valor[i+1]        % p.e. ≤1 moneda de 10000 cts, etc.
  );

%%  Estrategia de búsqueda 
% Explorar primero las monedas grandes, eligiendo el valor máximo

solve :: int_search(n, first_fail, indomain_max, complete)
      minimize numMonedas;

output [
  "monedas: ", show(n),          % vector en el orden 1 c … 5 U
  "\n",
  "total monedas: ", show(numMonedas)
];
