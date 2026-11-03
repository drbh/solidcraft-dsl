// 63. Thin-walled vase: a smooth spline profile (foot, belly, neck, lip) turned about Z, with a
// wall of even thickness measured square to the surface.
//
// A solid revolve of the profile can't be hollowed evenly: a cut by a second, smaller profile
// only sets the wall in sideways, so it thins where the side leans. Here the profile is a single
// open spline revolved as a surface (walls only, no caps), and `thicken` grows it by `wall` into
// a solid: inward, since this revolved surface's normal points toward the axis (`flip` would
// grow it outward). A floor disc, ending inside the wall, is joined to it, and the surface is
// removed, leaving one body.
//
// Thicken a revolve, extrude or sweep when the result is joined or cut later: the engine can
// thicken a loft, but can't join or cut the result (0.19 and 0.27).
//
// Print upright without supports. The wall ends square to the surface, so the lip's inner edge
// rises about 0.6 mm above `h`.
design ThinWallVase units mm
param foot_r = 35 mm
param belly_r = 50 mm
param neck_r = 30 mm
param lip_r = 36 mm
param belly_z = 60 mm
param neck_z = 130 mm
param h = 150 mm
param wall = 1.6 mm
param floor_t = 2 mm

// Profile on XZ (sketch coordinates are (x, -z))
sketch Profile on XZ {
  spline s = (foot_r, 0) (belly_r, -belly_z) (neck_r, -neck_z) (lip_r, -h)
  fix s
}
revolve Skin from Profile axis Z curves s surface -> body Skin
thicken Wall bodies Skin thickness wall new -> body Vase
remove DropSkin bodies Skin

// The floor, ending halfway into the wall
cylinder Floor radius (foot_r - wall/2) height floor_t base (0,0,0) join targets Vase

check Vase overhang <= 45 deg
