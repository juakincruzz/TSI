int: importe_cts;             % p.e. 147 para 1,47 U
set of int: T = 1..9;
array[T] of int: valor = [1,3,5,10,25,50,100,200,500];

array[T] of var 0..importe_cts: n;
var int: numMonedas = sum(t in T)(n[t]);

constraint sum(t in T)(valor[t] * n[t]) = importe_cts;

output [
  "monedas: ", show(n),          % vector en el orden 1 c … 5 U
  "\n",
  "total monedas: ", show(numMonedas)
];

solve satisfy;