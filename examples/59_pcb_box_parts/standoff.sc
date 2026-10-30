// A PCB standoff: a post with a blind hole for a self-tapping screw, standing on z = 0.
// Used by 59_pcb_box.sc; it builds on its own too.
design Standoff units mm
param od = 6 mm
param hole_d = 2.4 mm
param h = 6 mm
// Solid at the bottom, so the hole stops above whatever the post stands on
param floor = 1 mm
cylinder Post radius (od/2) height h base (0,0,0) -> body Standoff
cylinder Hole radius (hole_d/2) height h base (0,0,floor) cut targets Standoff
check Standoff size.z == h
