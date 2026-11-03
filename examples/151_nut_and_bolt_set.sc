// 151. Printable metric nut and bolt set, M8 to M12 coarse with real modelled threads: hex or socket heads.
//
// One bolt and its nut are drawn once, in 151_nut_and_bolt_set_parts/fastener.sc, and placed for
// each size. The part holds the ISO table: give it the nominal `d` (8, 10, 12, 16 or 20 mm) and it
// takes the coarse pitch, width across flats, head height, nut height and socket key from it, so
// a row of the set is one `use` line. `head` picks hex heads (1, ISO 4017 proportions) or socket
// caps (2, ISO 4762, with a 120 deg drill point closing the socket). Every bolt stands on its head
// and every nut lies flat, the way they print; the rows run along Y, bolt at x = 0, nut beside it.
//
// The fit is made as in 49_bottle_and_cap and 85_mason_jar: the shaft is turned `thread_play`
// under the major diameter so the crests clear, and the nut's thread `clearance` scales the nut out
// about its axis to open the flanks by `thread_clear`. On a thread this small that scale is about
// 5 %, so the part draws the nut that much smaller first: it comes out at the standard width
// across flats for a spanner, its bore about 0.1 mm over the minor. 0.25 mm suits a 0.4 mm nozzle;
// raise it if the nut binds. Bolts and nuts lie apart, as printed, and are not clearance-checked
// screwed together: near the meshing angle the engine's exact query doesn't finish (see 49).
//
// Print the bolts upright on their heads (the 60 deg flanks print as 30 deg overhangs, and the
// head's top corners are chamfered 30 deg) and the nuts flat, in 0.12 to 0.16 mm layers. PLA or
// PETG; these are for jigs, toys and knobs, not for load.
design NutAndBoltSet units mm

param head = 1                 // 1 hex head, 2 socket cap
param length = 30 mm           // bolt length under the head
param thread_play = 0.2 mm
param thread_clear = 0.25 mm
param row = 36 mm              // spacing of the rows on the plate

use M8 from "151_nut_and_bolt_set_parts/fastener.sc" with (d = 8 mm, length = length, head = head, thread_play = thread_play, thread_clear = thread_clear, nut_x = 26 mm) at (0, 0, 0)
use M10 from "151_nut_and_bolt_set_parts/fastener.sc" with (d = 10 mm, length = length, head = head, thread_play = thread_play, thread_clear = thread_clear, nut_x = 30 mm) at (0, row, 0)
use M12 from "151_nut_and_bolt_set_parts/fastener.sc" with (d = 12 mm, length = length, head = head, thread_play = thread_play, thread_clear = thread_clear, nut_x = 34 mm) at (0, 2*row, 0)

// Each size: the bolt's length and the nut's spanner size as drawn
check M8_Bolt size.z >= (length + 5 mm)
check M8_Nut size.x <= 13.05 mm
check M10_Nut size.x <= 16.05 mm
check M12_Nut size.x <= 18.05 mm
check M8_Nut clearance M8_Bolt >= 2 mm
check M10_Nut clearance M10_Bolt >= 2 mm
check M12_Nut clearance M12_Bolt >= 2 mm
check M10_Bolt clearance M8_Nut >= 2 mm
check M12_Bolt clearance M10_Nut >= 2 mm

color M8_Bolt, M10_Bolt, M12_Bolt "#c9cdd2"
color M8_Nut, M10_Nut, M12_Nut "#e0a526"
