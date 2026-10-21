// 1. Mixed units and arithmetic in feature sizes. V = pi * 6² * 12.
design Spacer units mm
param OD = 12 mm
param height = 1.2 cm
let radius = OD/2
cylinder Spacer radius radius height height -> body SpacerBody
