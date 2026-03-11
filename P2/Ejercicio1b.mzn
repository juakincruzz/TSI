
include "globals.mzn";

/*  Parámetro de entrada  */
int: importe_cts;                       % p. ej. 147 se pasa por .dzn

/*  Datos del problema  */
set of int: T = 1..9;
array[T] of int: valor = [  1,   3,   5,  10,  25,  50,
                          100, 200, 500 ];

/*  Constantes derivadas  */
int: U = importe_cts div 100;           % parte entera en Unidades

/*  Variables  */
array[T] of var 0..importe_cts: n;       % nº de monedas de cada tipo
var int: numMonedas = sum(t in T)(n[t]);

/*  Restricciones  */
constraint
      /* valor total = importe */
      sum(t in T)(valor[t] * n[t]) = importe_cts
/\    /* la parte entera se cubre SOLO con 1 U (idx 7) y 2 U (idx 8) */
      n[7] + 2*n[8] = U
/\    /* cotas individuales para acelerar la búsqueda (opcional) */
      forall(t in T)( n[t] <= importe_cts div valor[t] );

/* ---------- Salida -------------------------------------------- */
output [
  "monedas: ", show(n),          % vector en el orden 1 c … 5 U
  "\n",
  "total monedas: ", show(numMonedas)
];

solve satisfy;
