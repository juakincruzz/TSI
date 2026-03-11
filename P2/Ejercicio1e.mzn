float: importe;                               % 1.47        (Unidades)
array[1..9] of float: valor = [0.01, 0.03, 0.05, 0.10, 0.25, 0.50, 1.00, 2.00, 5.00];
       
array[1..9] of var 0.0..importe: n;           % nº de monedas 
var float: numMonedas = sum(i in 1..9)(n[i]);

constraint sum(i in 1..9)(valor[i] * n[i]) = importe;

output [ "importe = ", show(importe), "\n",
         "numMonedas = ", show(numMonedas), "\n",
         "n = ", show(n) ];

solve satisfy;          
