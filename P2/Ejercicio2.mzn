include "globals.mzn";

% Definición de enumeraciones para los nombres y ciudades
enum Nombres1 = {Lucia, Sofia, Juan, Cristina, Claudia};
enum Nombres2 = {Pablo, Marcos, Jose, Marta, Pedro};
enum Ciudades = {Madrid, Barcelona, Sevilla, Lyon, Parma};

% Variables: quién está casado con quién
array[Nombres1] of var Nombres2: pareja;
% Variables: de qué ciudad es cada pareja
array[Nombres1] of var Ciudades: ciudad;
% Variables: años de matrimonio (máximo 30 años según la pista 4)
array[Nombres1] of var 1..30: anios;

% Restricciones de asignación única
constraint alldifferent(pareja);
constraint alldifferent(ciudad);
constraint alldifferent(anios);

% Variables inversas para facilitar el acceso a los datos
array[Nombres2] of var Nombres1: n2_pareja;
constraint forall(n1 in Nombres1) (n2_pareja[pareja[n1]] = n1);

array[Ciudades] of var Nombres1: c_persona;
constraint forall(n1 in Nombres1) (c_persona[ciudad[n1]] = n1);

% Pista 1: Sofía no lleva casada tanto tiempo como José o los de Barcelona, pero sí más que Marcos o los Madrid.
constraint anios[Sofia] < anios[n2_pareja[Jose]];
constraint anios[Sofia] < anios[c_persona[Barcelona]];
constraint anios[Sofia] > anios[n2_pareja[Marcos]];
constraint anios[Sofia] > anios[c_persona[Madrid]];

% Pista 2: Cristina lleva casada el doble de tiempo que los de Sevilla, pero solo la mitad que Pablo.
constraint anios[Cristina] = 2 * anios[c_persona[Sevilla]];
constraint 2 * anios[Cristina] = anios[n2_pareja[Pablo]];

% Pista 3: Los de Parma llevan casados diez años más que Marta y diez años menos que Juan.
constraint anios[c_persona[Parma]] = anios[n2_pareja[Marta]] + 10;
constraint anios[c_persona[Parma]] = anios[Juan] - 10;

% Pista 4: Marcos y Claudia llevan 25 años menos de casados que los de Lyon, quienes, con 30 años de casados, son la pareja que lleva más tiempo casada.
constraint anios[n2_pareja[Marcos]] = anios[c_persona[Lyon]] - 25;
constraint anios[Claudia] = anios[c_persona[Lyon]] - 25;
constraint anios[c_persona[Lyon]] = 30;

% Pista 5: Ni Lucía ni los de Madrid son los que llevan casados menos tiempo.
constraint anios[Lucia] > min([anios[n] | n in Nombres1]);
constraint anios[c_persona[Madrid]] > min([anios[n] | n in Nombres1]);

% Pista 6: Pedro lleva 25 años casado.
constraint anios[n2_pareja[Pedro]] = 25;

% Resolver el problema
solve satisfy;

% Mostrar el resultado formateado
output [
  show(c) ++ ": " ++ show(c_persona[c]) ++ " y " ++ show(pareja[c_persona[c]]) ++ " " ++ show(anios[c_persona[c]]) ++ " años\n" 
  | c in Ciudades
];