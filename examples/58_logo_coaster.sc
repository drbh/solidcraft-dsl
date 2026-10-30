// 58. Drink coaster with a logo drawn in another tool: the outline comes from an SVG file beside
// the design, raised from the coaster face so it can be printed in a second colour.
//
// `svg` reads 58_logo_coaster_art/mountains.svg when the design is built and puts its curves in
// the sketch. SVG measures y downwards, so a drawing lands below its origin as it appears on
// screen; `at` places its top-left corner. The logo's curves are not constrained: the file is
// the source of truth for them. A rim keeps drips on the coaster.
//
// Print flat, face up; change filament at the top of the face for a two-colour logo.
design LogoCoaster units mm
param D = 96 mm
param base_t = 3 mm
param rim_w = 3 mm
param rim_h = 1.5 mm
param logo_h = 1 mm
// The drawing is 44 x 26 mm; centre it
param logo_w = 44 mm
param logo_d = 26 mm

cylinder Disc radius (D/2) height base_t base (0,0,0) -> body Coaster
sketch RimSk on XY offset base_t {
  circle o = (0,0) radius (D/2)
  circle i = (0,0) radius (D/2 - rim_w)
  fix o i
}
extrude Rim from RimSk distance rim_h profiles (D/2 - rim_w/2,0) join targets Coaster

sketch Logo on XY offset base_t {
  svg "58_logo_coaster_art/mountains.svg" at (-logo_w/2,logo_d/2)
}
extrude Raised from Logo distance logo_h join targets Coaster
color Coaster "#2f6f4f"

check Coaster overhang <= 45 deg
check Coaster size.z == (base_t + rim_h)
