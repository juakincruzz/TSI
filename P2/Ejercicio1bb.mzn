int: importe_cts;             % p.e. 147 para 1,47 U
set of int: T = 1..9;
array[T] of int: valor = [1,3,5,10,25,50,100,200,500];

array[T] of var 0..importe_cts: n;

var int: numMonedas = sum(t in T)(n[t]);


% necesito las Unidades div 100 para decirle 
 int: U = importe_cts div 100;
% Ya sé cuántas unidades puede haber
%% Tengo que ver si puedo expresar importe_cts = U + 2*U

constraint sum(t in T)(valor[t] * n[t]) = importe_cts;

constraint n[7] + 2*n[8] = U;

output [
  "monedas: ", show(n),          % vector en el orden 1 c … 5 U
  "\n",
  "total monedas: ", show(numMonedas)
];

solve satisfy;