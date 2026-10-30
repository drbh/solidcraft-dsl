// 39. Snap-fit enclosure for an ESP32 DevKitC (V4, about 55 x 28 mm, no mounting holes).
//
// Origin is the centre of the base floor's underside, X runs along the board with the micro-USB
// end at -X, Z is up. The DevKitC has no mounting holes and its pin headers point down, so the
// board drops into a pocket that matches its outline (board_fit all round) and rests on two end
// rails between the header rows, high enough for the header pins. The lid carries four fins that
// come down at the board's corners to hold it on the rails, a tongue that closes the top of the
// USB notch, and four cantilever tabs whose hooks click into windows in the long walls.
// Tabs are tab_len long and tab_t thick for a hook of hook_d: about 2% strain, fine for PLA/PETG.
// Release by pressing the hooks in through the windows.
//
// Print both parts open side up, no supports. The base stands on its floor: walls, rails and the
// USB notch are vertical, and the only bridge is the top of each tab window (tab_w + 2*snap_gap).
// The lid prints upside down on its plate: fins, tongue and tabs rise straight from the plate and
// both hook flanks are 45 deg. Vent slots go straight through the floor and the lid plate, and
// the label is debossed into the lid face that lies on the bed.
design Esp32Enclosure units mm

// Measure these on the board
param board_l = 55 mm
param board_w = 28 mm
param board_t = 1.6 mm
param pins_below = 9.5 mm
param parts_above = 9 mm
param header_keepout = 3.5 mm
param usb_w = 11 mm
param usb_z = 1.5 mm
param usb_h = 8 mm

// Shell
param wall = 2 mm
param floor_t = 2 mm
param lid_t = 2 mm
param board_fit = 0.4 mm
param corner_r = 3 mm
param rail_len = 3 mm

// Snap tabs
param tab_w = 8 mm
param tab_t = 1.6 mm
param tab_len = 10 mm
param tab_x = 15 mm
param hook_d = 0.8 mm
param snap_gap = 0.2 mm

// Lid fins and vents
param fin_t = 1.6 mm
param fin_y0 = 9.5 mm
param fin_y1 = 13.5 mm
param fin_gap = 0.2 mm
param vent_w = 2 mm
param vent_len = 30 mm
param vent_pitch = 4 mm

let in_l = board_l + 2*board_fit
let in_w = board_w + 2*board_fit
let out_l = in_l + 2*wall
let out_w = in_w + 2*wall
let rail_z = floor_t + pins_below
let board_top = rail_z + board_t
let rim_z = board_top + parts_above
let tab_tip = rim_z - tab_len
let tab_y = in_w/2 - snap_gap
let usb_c = board_top + usb_z

// Base: block with rounded vertical corners, then the board pocket
box BaseBlock size out_l out_w rim_z corner (-out_l/2,-out_w/2,0) -> body Base
fillet BaseCorners edges (out_l/2,out_w/2,rim_z/2) (-out_l/2,out_w/2,rim_z/2) (-out_l/2,-out_w/2,rim_z/2) (out_l/2,-out_w/2,rim_z/2) radius corner_r body Base
box Pocket size in_l in_w (rim_z) corner (-in_l/2,-in_w/2,floor_t) cut targets Base

// End rails under the board's short ends, between the header rows
let rail_hw = board_w/2 - header_keepout
box RailUsb size rail_len (2*rail_hw) pins_below corner (-in_l/2,-rail_hw,floor_t) join targets Base
box RailAnt size rail_len (2*rail_hw) pins_below corner (in_l/2-rail_len,-rail_hw,floor_t) join targets Base

// USB notch, open at the top; the lid's tongue closes it above the plug
let notch_w = usb_w + 2*snap_gap
box UsbNotch size (wall+2 mm) notch_w (rim_z) corner (-out_l/2-1 mm,-notch_w/2,usb_c-usb_h/2) cut targets Base

// Windows for the tab hooks: two in each long wall
let win_w = tab_w + 2*snap_gap
let win_z = tab_tip - snap_gap
let win_h = 2*hook_d + 2*snap_gap + 0.6 mm
box WinA size win_w (wall+2 mm) win_h corner (tab_x-win_w/2,in_w/2-1 mm,win_z) cut targets Base
box WinB size win_w (wall+2 mm) win_h corner (-tab_x-win_w/2,in_w/2-1 mm,win_z) cut targets Base
box WinC size win_w (wall+2 mm) win_h corner (tab_x-win_w/2,-out_w/2-1 mm,win_z) cut targets Base
box WinD size win_w (wall+2 mm) win_h corner (-tab_x-win_w/2,-out_w/2-1 mm,win_z) cut targets Base

// Floor vents
sketch FloorVentSk on XY offset (-1 mm) {
  slot v0 = centers (-vent_len/2,0) (vent_len/2,0) width vent_w
  slot v1 = centers (-vent_len/2,vent_pitch) (vent_len/2,vent_pitch) width vent_w
  slot v2 = centers (-vent_len/2,-vent_pitch) (vent_len/2,-vent_pitch) width vent_w
  fix v0.line1 v0.line2 v0.arc1 v0.arc2 v1.line1 v1.line2 v1.arc1 v1.arc2 v2.line1 v2.line2 v2.arc1 v2.arc2
}
extrude FloorVents from FloorVentSk distance (floor_t+2 mm) cut targets Base

// Lid: plate on the rim with the same rounded corners
box LidPlate size out_l out_w lid_t corner (-out_l/2,-out_w/2,rim_z) new -> body Lid
fillet LidCorners edges (out_l/2,out_w/2,rim_z+lid_t/2) (-out_l/2,out_w/2,rim_z+lid_t/2) (-out_l/2,-out_w/2,rim_z+lid_t/2) (out_l/2,-out_w/2,rim_z+lid_t/2) radius corner_r body Lid

// Cantilever tabs hanging inside the long walls, hooks pointing out (45 deg flanks)
box TabA size tab_w tab_t tab_len corner (tab_x-tab_w/2,tab_y-tab_t,tab_tip) join targets Lid
box TabB size tab_w tab_t tab_len corner (-tab_x-tab_w/2,tab_y-tab_t,tab_tip) join targets Lid
box TabC size tab_w tab_t tab_len corner (tab_x-tab_w/2,-tab_y,tab_tip) join targets Lid
box TabD size tab_w tab_t tab_len corner (-tab_x-tab_w/2,-tab_y,tab_tip) join targets Lid
sketch HookSk on YZ offset (-tab_x-tab_w/2) {
  polyline closed hp = (tab_y-0.01 mm,tab_tip) (tab_y+hook_d,tab_tip+hook_d) (tab_y-0.01 mm,tab_tip+2*hook_d)
  polyline closed hn = (-tab_y+0.01 mm,tab_tip) (-tab_y+0.01 mm,tab_tip+2*hook_d) (-tab_y-hook_d,tab_tip+hook_d)
  fix hp.1 hp.2 hp.3 hn.1 hn.2 hn.3
}
extrude HookB from HookSk distance tab_w join targets Lid
sketch HookSk2 on YZ offset (tab_x-tab_w/2) {
  polyline closed hp = (tab_y-0.01 mm,tab_tip) (tab_y+hook_d,tab_tip+hook_d) (tab_y-0.01 mm,tab_tip+2*hook_d)
  polyline closed hn = (-tab_y+0.01 mm,tab_tip) (-tab_y+0.01 mm,tab_tip+2*hook_d) (-tab_y-hook_d,tab_tip+hook_d)
  fix hp.1 hp.2 hp.3 hn.1 hn.2 hn.3
}
extrude HookA from HookSk2 distance tab_w join targets Lid

// Hold-down fins at the board corners, against the end walls; they stop fin_gap above the board
let fin_h = rim_z - board_top - fin_gap
let fin_wid = fin_y1 - fin_y0
box FinA size fin_t fin_wid fin_h corner (in_l/2-fin_t-fin_gap,fin_y0,board_top+fin_gap) join targets Lid
box FinB size fin_t fin_wid fin_h corner (in_l/2-fin_t-fin_gap,-fin_y1,board_top+fin_gap) join targets Lid
box FinC size fin_t fin_wid fin_h corner (-in_l/2+fin_gap,fin_y0,board_top+fin_gap) join targets Lid
box FinD size fin_t fin_wid fin_h corner (-in_l/2+fin_gap,-fin_y1,board_top+fin_gap) join targets Lid

// Tongue that fills the USB notch above the plug
let tongue_bot = usb_c + usb_h/2 + snap_gap
box Tongue size wall (usb_w) (rim_z-tongue_bot) corner (-out_l/2,-usb_w/2,tongue_bot) join targets Lid

// Lid vents
sketch LidVentSk on XY offset (rim_z-1 mm) {
  slot v0 = centers (-vent_len/2,0) (vent_len/2,0) width vent_w
  slot v1 = centers (-vent_len/2,vent_pitch) (vent_len/2,vent_pitch) width vent_w
  slot v2 = centers (-vent_len/2,-vent_pitch) (vent_len/2,-vent_pitch) width vent_w
  slot v3 = centers (-vent_len/2,2*vent_pitch) (vent_len/2,2*vent_pitch) width vent_w
  slot v4 = centers (-vent_len/2,-2*vent_pitch) (vent_len/2,-2*vent_pitch) width vent_w
  fix v0.line1 v0.line2 v0.arc1 v0.arc2 v1.line1 v1.line2 v1.arc1 v1.arc2 v2.line1 v2.line2 v2.arc1 v2.arc2 v3.line1 v3.line2 v3.arc1 v3.arc2 v4.line1 v4.line2 v4.arc1 v4.arc2
}
extrude LidVents from LidVentSk distance (lid_t+2 mm) cut targets Lid

// Label debossed into the lid top beside the vents (on the bed when printed, so the letters are
// shallow recesses in the first layers).
param label_h = 5 mm
param label_depth = 0.6 mm
sketch LabelSk on XY offset (rim_z+lid_t) {
  text "ESP32" at ((vent_len+out_l)/4,0) height label_h angle 90 deg align center
}
emboss Label from LabelSk depth label_depth deboss targets Lid

// Print orientations (also used by the overhang checks); prints without supports.
check Base overhang <= 45 deg
print Lid up (0, 0, -1)
check Lid overhang <= 45 deg
