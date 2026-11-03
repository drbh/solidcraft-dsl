// The shared joint of 108_car_track: a flat dovetail tab in the track floor, drawn once.
//
// Local frame: the piece's end face is x = 0, the tab reaches out along +X. grow = 0 is the tab;
// grow = clearance is the socket cutter, the outline pushed out on every side, which the next
// piece cuts through its floor so the tab drops in flush and locks against pulling apart.
design CarTab units mm

param neck_w = 10 mm
param head_w = 16 mm
param reach = 10 mm
param overlap = 0.8 mm
param grow = 0 mm
param z0 = 0.1 mm
param h = 1.8 mm

// The outline grown by `grow` square to every edge: the slanted flanks move out along their
// normal, then meet the moved tip and back edges
let nh = neck_w/2
let hh = head_w/2
let run = reach + overlap
let len = sqrt(run^2 + (hh - nh)^2)
let m = (hh - nh)/run                       // the flank's slope, widening outward
let fx = -overlap - grow*(hh - nh)/len      // a point on the moved lower flank
let fy = -nh - grow*run/len
let y_tip = fy - m*(reach + grow - fx)
let y_back = fy - m*(-overlap - grow - fx)

sketch Outline on XY offset z0 {
  polyline closed p = (-overlap - grow, y_back) (reach + grow, y_tip) (reach + grow, -y_tip) (-overlap - grow, -y_back)
  fix p
}
extrude Tab from Outline distance h new -> body Tab
color Tab "#f26b1d"
