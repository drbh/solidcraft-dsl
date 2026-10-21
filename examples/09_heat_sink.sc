// 9. A parametric fin array with computed integer topology and a driven placement.
// Count is evaluated when compiling; change the source and rebuild to change topology.
design HeatSink units mm
param L = 80 mm
param W = 50 mm
param base_t = 5 mm
param fin_t = 2 mm
param fin_h = 20 mm
param gap = 4 mm
let count = floor((W-gap)/(fin_t+gap))+1
let array_width = (count-1)*(fin_t+gap)+fin_t
let margin = (W-array_width)/2
box Base size L W base_t -> body HeatSinkBody
box FirstFin size L fin_t fin_h corner (0,margin,base_t) join targets HeatSinkBody
pattern Fins features FirstFin dir (0,1,0) count count spacing (fin_t+gap)
