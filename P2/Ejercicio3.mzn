include "globals.mzn";

int: IDX = 12;

constraint grid[1,2] = 1;
constraint grid[1,3] = 0;
constraint grid[1,8] = 0;
constraint grid[1,11] = 0;

constraint grid[2,2] = 1;
constraint grid[2,3] = 1;
constraint grid[2,6] = 1;

constraint grid[3,9] = 1;
constraint grid[3,12] = 0;

constraint grid[4,3] = 0;
constraint grid[4,4] = 0;
constraint grid[4,12] = 0;

constraint grid[5,7] = 1;
constraint grid[5,8] = 1;
constraint grid[5,10] = 0;

constraint grid[6,2] = 1;
constraint grid[6,4] = 0;
constraint grid[6,6] = 1;
constraint grid[6,7] = 1;
constraint grid[6,11] = 1;

constraint grid[7,9] = 1;

constraint grid[8,1] = 1;
constraint grid[8,4] = 1;
constraint grid[8,11] = 0;

constraint grid[9,2] = 1;
constraint grid[9,9] = 0;

constraint grid[10,8] = 0;

constraint grid[11,1] = 1;
constraint grid[11,12] = 1;

constraint grid[12,2] = 1;
constraint grid[12,4] = 1;
constraint grid[12,11] = 0;
constraint grid[12,10] = 0;


% Variables de decisión
array[1..IDX,1..IDX] of var 0..1: grid;



% Restricción 2: 6 unos/ceros por fila/columna
constraint forall(i in 1..IDX)( sum(j in 1..IDX)(grid[i,j]) = IDX div 2 /\ sum(j in 1..IDX)(grid[j,i]) = IDX div 2);

% Restricción 3: Filas y columnas únicas (versión numérica)
constraint forall(i, j in 1..IDX where i < j)(
  exists(k in 1..IDX)(grid[i,k] != grid[j,k]) /\ exists(k in 1..IDX)(grid[k,i] != grid[k,j])); 
% Restricción 4: No 3 consecutivos
constraint forall(i in 1..IDX, j in 1..(IDX-2))(
    not (grid[i,j] = grid[i,j+1] /\ grid[i, j+1] = grid[i,j+2]) /\ not (grid[j,i] = grid[j+1,i] /\ grid[j+1,i] = grid[j+2,i])
);



solve satisfy;

output [
    if j = 1 then "Fila " ++ show(i) ++ ": " else "" endif ++
    show(grid[i,j]) ++
    if j = IDX then "\n" else " " endif
    | i in 1..IDX, j in 1..IDX
];