// 30. Open-top box, 6 in wide inside x 3 in long x 3.5 in tall (outside), not load-bearing.
// Walls and floor are 0.8 mm: two perimeters for a 0.4 mm nozzle, the thinnest that
// still prints as a closed wall. The top face is removed by the shell.
design OpenBox units mm
param wall = 0.8 mm
param W = 6 in + 2*wall   // outside width; 6 in inside
param L = 3 in
param H = 3.5 in
box Outer size W L H corner (0,0,0) -> body OpenBox
shell Cavity faces (W/2,L/2,H) thickness wall inside body OpenBox

// Prints without supports, in the print recipe's orientation.
check OpenBox overhang <= 45 deg
