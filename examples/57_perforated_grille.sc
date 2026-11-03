// 57. Perforated sheet-metal grille: an aluminium plate with a staggered field of holes and a
// mounting lip bent down along each long edge, the lips' raw edges hemmed so they are safe to
// handle.
//
// Modelled with the plate lying on XY and the lips bent up along y = 0 and y = W. A hem folds
// each lip's edge back on itself. The perforation is a field over the flat sheet: it is in the
// flat pattern, the DXF (layer PERFORATION) and the mass, not cut into the solid. Export the flat
// pattern from the print recipe and send it to a laser or punch; bend the lips, then the hems.
design PerforatedGrille units mm
param L = 160 mm
param W = 90 mm
param t = 1 mm
param lip = 15 mm
param hem_len = 5 mm
param hole_d = 4 mm
param pitch = 6.5 mm
param margin = 8 mm
sheet_rule Aluminium thickness t bend_radius t k_factor 0.44

sketch Blank on XY {
  rect r = (0,0) (L,W)
  fix r
}
flange Plate from Blank profiles (2 mm, 2 mm) rule Aluminium -> body Grille
flange FrontLip edges (L/2,0,0) height lip flip body Grille
flange BackLip edges (L/2,W,0) height lip flip body Grille
hem FrontHem edges (L/2,0,lip) length hem_len body Grille
hem BackHem edges (L/2,W,lip) length hem_len body Grille
perforate Holes diameter hole_d pitch pitch margin margin body Grille

check Grille size.x == L
