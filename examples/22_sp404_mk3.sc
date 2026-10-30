// 22. SP-404 MK3 style sampler, buildable: printed tray and top panel, printed pads, caps and
// knobs, one control PCB on standoffs, rear and front I/O sized for real panel parts.
//
// Proportions follow the SP-404MKII (179 x 296 mm footprint); the MK3 layout is a guess, so treat
// the panel positions as parameters to adjust. X runs left to right across the panel, Y from the
// front edge (y = 0, pads) to the rear (y = D, jacks), Z is up from the underside of the tray.
//
// Panel, front to rear:
//   pad field     4 x 4 velocity pads (TPU) over FSRs, in a 1 mm well
//   bank row      five bank buttons (A/F .. E/J) above the pads
//   left column   six function buttons beside the pads
//   effect row    six effect buttons
//   control strip VOLUME, CTRL 1-3, OLED window and the VALUE encoder
//
// Stack under the panel (z, top down): panel 37-40, cap and pad flanges 36-37, 6x6x5 mm tactile
// switches and 9 mm pots on the control PCB, whose top face is at 31; FSRs (0.3 mm) lie on the
// PCB under the pads. The PCB sits on eight standoffs with M3 heat-set inserts; the Teensy 4.1 and
// audio adaptor hang under it. The panel screws down with six countersunk M3 x 8 into inserts in
// the tray bosses.
//
// Print: Tray floor down; Panel top up (flat underside); Pads in TPU 95A, flange down; caps in
// translucent PETG, flange down; knobs base down. ControlPcb, Display and Feet are reference
// bodies (order the PCB, buy the bumpers). The tray needs a 300 mm bed (Prusa XL, Bambu H2D).
design SP404MK3 units mm

// Tray: rounded footprint, shelled from the top
param W = 179 mm
param D = 296 mm
param H = 40 mm
param corner_r = 10 mm
param wall = 2.5 mm
param top_t = 3 mm
param top_chamfer = 1.5 mm
let split_z = H - top_t
sketch FootprintSk on XY {
  rect rounded outline = (0,0) (W,D) radius corner_r
  fix outline.bottom outline.right outline.top outline.left outline.arc1 outline.arc2 outline.arc3 outline.arc4
}
extrude TrayBlock from FootprintSk distance split_z -> body Tray
shell TrayCavity faces (W/2,D/2,split_z) thickness wall inside body Tray

// Panel screw bosses in the tray corners and beside the pad field, M3 inserts (4 mm x 6 mm)
param boss_r = 3.5 mm
param boss_inset = 7 mm
param insert_r = 2 mm
param insert_depth = 6 mm
let mid_y = (pads_y + field + pad_lip + bank_y - btn_lip)/2
let boss_h = split_z - wall
let insert_z = split_z - insert_depth
let bx0 = boss_inset
let bx1 = W - boss_inset
let by0 = boss_inset
let by2 = D - boss_inset
cylinder Boss0 radius boss_r height boss_h base (bx0,by0,wall) join targets Tray
cylinder Boss1 radius boss_r height boss_h base (bx1,by0,wall) join targets Tray
cylinder Boss2 radius boss_r height boss_h base (bx0,mid_y,wall) join targets Tray
cylinder Boss3 radius boss_r height boss_h base (bx1,mid_y,wall) join targets Tray
cylinder Boss4 radius boss_r height boss_h base (bx0,by2,wall) join targets Tray
cylinder Boss5 radius boss_r height boss_h base (bx1,by2,wall) join targets Tray
cylinder Insert0 radius insert_r height (insert_depth+1 mm) base (bx0,by0,insert_z) cut targets Tray
cylinder Insert1 radius insert_r height (insert_depth+1 mm) base (bx1,by0,insert_z) cut targets Tray
cylinder Insert2 radius insert_r height (insert_depth+1 mm) base (bx0,mid_y,insert_z) cut targets Tray
cylinder Insert3 radius insert_r height (insert_depth+1 mm) base (bx1,mid_y,insert_z) cut targets Tray
cylinder Insert4 radius insert_r height (insert_depth+1 mm) base (bx0,by2,insert_z) cut targets Tray
cylinder Insert5 radius insert_r height (insert_depth+1 mm) base (bx1,by2,insert_z) cut targets Tray

// Top panel with countersunk M3 holes over the bosses
sketch PanelSk on XY offset split_z {
  rect rounded outline = (0,0) (W,D) radius corner_r
  fix outline.bottom outline.right outline.top outline.left outline.arc1 outline.arc2 outline.arc3 outline.arc4
}
extrude PanelPlate from PanelSk distance top_t new -> body Panel
chamfer TopEdge edges (W/2,0,H) distance top_chamfer body Panel
param screw_d = 3.4 mm
param screw_head = 6.5 mm
hole PanelScrew0 at (bx0,by0,H) diameter screw_d countersink screw_head 90 deg depth top_t
hole PanelScrew1 at (bx1,by0,H) diameter screw_d countersink screw_head 90 deg depth top_t
hole PanelScrew2 at (bx0,mid_y,H) diameter screw_d countersink screw_head 90 deg depth top_t
hole PanelScrew3 at (bx1,mid_y,H) diameter screw_d countersink screw_head 90 deg depth top_t
hole PanelScrew4 at (bx0,by2,H) diameter screw_d countersink screw_head 90 deg depth top_t
hole PanelScrew5 at (bx1,by2,H) diameter screw_d countersink screw_head 90 deg depth top_t

// Under-panel stack: flanges, tactile switches, PCB
param flange_t = 1 mm
param switch_h = 5 mm
param pcb_t = 1.6 mm
param fsr_t = 0.3 mm
param fit = 0.4 mm
let pcb_top = split_z - flange_t - switch_h
let pcb_bottom = pcb_top - pcb_t

// Pad field: 4 x 4 pads in a 1 mm well, each through its own hole
param pad = 30 mm
param pad_gap = 6 mm
param pad_r = 3 mm
param pad_rise = 3 mm
param pad_lip = 2 mm
param well_depth = 1 mm
param well_margin = 4 mm
param pads_x = 34 mm
param pads_y = 16 mm
let pitch = pad + pad_gap
let field = 4*pad + 3*pad_gap
sketch PadWellSk on XY offset H {
  rect rounded well = (pads_x-well_margin,pads_y-well_margin) (pads_x+field+well_margin,pads_y+field+well_margin) radius (pad_r+well_margin)
  fix well.bottom well.right well.top well.left well.arc1 well.arc2 well.arc3 well.arc4
}
extrude PadWell from PadWellSk distance well_depth negative cut targets Panel
sketch PadHoleSk on XY offset H {
  rect rounded h = (pads_x-fit,pads_y-fit) (pads_x+pad+fit,pads_y+pad+fit) radius (pad_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude PadHole from PadHoleSk distance (top_t+1 mm) negative cut targets Panel
pattern PadHoles features PadHole dir (1,0,0) count 4 spacing pitch dir2 (0,1,0) count2 4 spacing2 pitch
// Pad: flange under the panel, body resting on its FSR, top pad_rise above the panel
let pad_base = pcb_top + fsr_t
sketch PadFlangeSk on XY offset (split_z-flange_t) {
  rect rounded f = (pads_x-pad_lip,pads_y-pad_lip) (pads_x+pad+pad_lip,pads_y+pad+pad_lip) radius (pad_r+pad_lip)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude PadFlange from PadFlangeSk distance flange_t new -> body Pads
sketch PadSk on XY offset pad_base {
  rect rounded p = (pads_x,pads_y) (pads_x+pad,pads_y+pad) radius pad_r
  fix p.bottom p.right p.top p.left p.arc1 p.arc2 p.arc3 p.arc4
}
extrude Pad from PadSk distance (H+pad_rise-pad_base) join targets Pads
pattern PadGrid bodies Pads dir (1,0,0) count 4 spacing pitch dir2 (0,1,0) count2 4 spacing2 pitch

// Buttons: printed caps through rounded holes, flange under the panel on a 6x6 tactile switch
param btn_w = 16 mm
param btn_h = 9 mm
param btn_r = 2 mm
param btn_rise = 2.5 mm
param btn_lip = 1.5 mm
let cap_h = top_t + btn_rise
let bank_y = pads_y + field + 12 mm
let bank_pitch = (field - btn_w)/4
sketch BankHoleSk on XY offset H {
  rect rounded h = (pads_x-fit,bank_y-fit) (pads_x+btn_w+fit,bank_y+btn_h+fit) radius (btn_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude BankHole from BankHoleSk distance (top_t+1 mm) negative cut targets Panel
pattern BankHoles features BankHole dir (1,0,0) count 5 spacing bank_pitch
sketch BankFlangeSk on XY offset (split_z-flange_t) {
  rect rounded f = (pads_x-btn_lip,bank_y-btn_lip) (pads_x+btn_w+btn_lip,bank_y+btn_h+btn_lip) radius (btn_r+btn_lip)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude BankFlange from BankFlangeSk distance flange_t new -> body BankButtons
sketch BankSk on XY offset split_z {
  rect rounded b = (pads_x,bank_y) (pads_x+btn_w,bank_y+btn_h) radius btn_r
  fix b.bottom b.right b.top b.left b.arc1 b.arc2 b.arc3 b.arc4
}
extrude BankCap from BankSk distance cap_h join targets BankButtons
pattern BankRow bodies BankButtons dir (1,0,0) count 5 spacing bank_pitch

// Left column: six function buttons beside the pads
param col_x = 10 mm
let col_pitch = (field - btn_h)/5
sketch ColumnHoleSk on XY offset H {
  rect rounded h = (col_x-fit,pads_y-fit) (col_x+btn_w+fit,pads_y+btn_h+fit) radius (btn_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude ColumnHole from ColumnHoleSk distance (top_t+1 mm) negative cut targets Panel
pattern ColumnHoles features ColumnHole dir (0,1,0) count 6 spacing col_pitch
sketch ColumnFlangeSk on XY offset (split_z-flange_t) {
  rect rounded f = (col_x-btn_lip,pads_y-btn_lip) (col_x+btn_w+btn_lip,pads_y+btn_h+btn_lip) radius (btn_r+btn_lip)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude ColumnFlange from ColumnFlangeSk distance flange_t new -> body FunctionButtons
sketch ColumnSk on XY offset split_z {
  rect rounded c = (col_x,pads_y) (col_x+btn_w,pads_y+btn_h) radius btn_r
  fix c.bottom c.right c.top c.left c.arc1 c.arc2 c.arc3 c.arc4
}
extrude ColumnCap from ColumnSk distance cap_h join targets FunctionButtons
pattern Column bodies FunctionButtons dir (0,1,0) count 6 spacing col_pitch

// Effect row: six buttons, centred between the bank row and the knobs
let fx_y = (bank_y + btn_h + strip_mid - skirt_r)/2 - btn_h/2
let fx_pitch = (W - 2*col_x - btn_w)/5
sketch EffectHoleSk on XY offset H {
  rect rounded h = (col_x-fit,fx_y-fit) (col_x+btn_w+fit,fx_y+btn_h+fit) radius (btn_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude EffectHole from EffectHoleSk distance (top_t+1 mm) negative cut targets Panel
pattern EffectHoles features EffectHole dir (1,0,0) count 6 spacing fx_pitch
sketch EffectFlangeSk on XY offset (split_z-flange_t) {
  rect rounded f = (col_x-btn_lip,fx_y-btn_lip) (col_x+btn_w+btn_lip,fx_y+btn_h+btn_lip) radius (btn_r+btn_lip)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude EffectFlange from EffectFlangeSk distance flange_t new -> body EffectButtons
sketch EffectSk on XY offset split_z {
  rect rounded e = (col_x,fx_y) (col_x+btn_w,fx_y+btn_h) radius btn_r
  fix e.bottom e.right e.top e.left e.arc1 e.arc2 e.arc3 e.arc4
}
extrude EffectCap from EffectSk distance cap_h join targets EffectButtons
pattern EffectRow bodies EffectButtons dir (1,0,0) count 6 spacing fx_pitch

// Control strip: OLED window through a 1 mm bezel; the 1.54" module sits under the panel
param oled_w = 36 mm
param oled_h = 19 mm
param oled_x = 92 mm
param bezel = 3 mm
param bezel_depth = 1 mm
param module_w = 44 mm
param module_h = 38 mm
param module_t = 4 mm
param strip_mid = 250 mm
let oled_y = strip_mid - oled_h/2
sketch BezelSk on XY offset H {
  rect rounded bz = (oled_x-bezel,oled_y-bezel) (oled_x+oled_w+bezel,oled_y+oled_h+bezel) radius bezel
  fix bz.bottom bz.right bz.top bz.left bz.arc1 bz.arc2 bz.arc3 bz.arc4
}
extrude Bezel from BezelSk distance bezel_depth negative cut targets Panel
box OledWindow size oled_w oled_h (top_t+1 mm) corner (oled_x,oled_y,split_z-0.5 mm) cut targets Panel
box OledModule size module_w module_h module_t corner (oled_x+oled_w/2-module_w/2,strip_mid-module_h/2,split_z-module_t) new -> body Display

// Knobs on 6 mm D shafts (9 mm pots, EC11 encoder), 1 mm above the panel. Shaft holes take the
// M7 bushings. Each knob: skirt, cap, pointer notch at 12 o'clock, D bore from below.
param knob_r = 8 mm
param knob_h = 14 mm
param skirt_r = 10 mm
param skirt_h = 2 mm
param pointer_w = 1.2 mm
param value_r = 11 mm
param vol_r = 7 mm
param ctrl_pitch = 26 mm
param bushing_r = 3.6 mm
param shaft_r = 3.1 mm
param shaft_flat = 1.5 mm
param bore_depth = 10 mm
let knob_z = H + 1 mm
let knob_y = strip_mid
let ctrl_x0 = col_x + skirt_r
let value_x = W - col_x - value_r
let vol_x = ctrl_x0
let vol_y = D - 16 mm
cylinder CtrlShaft radius bushing_r height (top_t+2 mm) base (ctrl_x0,knob_y,split_z-1 mm) cut targets Panel
pattern CtrlShafts features CtrlShaft dir (1,0,0) count 3 spacing ctrl_pitch
cylinder ValueShaft radius bushing_r height (top_t+2 mm) base (value_x,knob_y,split_z-1 mm) cut targets Panel
cylinder VolumeShaft radius bushing_r height (top_t+2 mm) base (vol_x,vol_y,split_z-1 mm) cut targets Panel

cylinder Ctrl1Skirt radius skirt_r height skirt_h base (ctrl_x0,knob_y,knob_z) new -> body CtrlKnobs
cylinder Ctrl1Cap radius knob_r height knob_h base (ctrl_x0,knob_y,knob_z) join targets CtrlKnobs
box Ctrl1Pointer size pointer_w knob_r (2 mm) corner (ctrl_x0-pointer_w/2,knob_y,knob_z+knob_h-1 mm) cut targets CtrlKnobs
cylinder Ctrl1Bore radius shaft_r height bore_depth base (ctrl_x0,knob_y,knob_z-1 mm) cut targets CtrlKnobs
box Ctrl1Flat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) bore_depth corner (ctrl_x0+shaft_flat,knob_y-shaft_r-0.1 mm,knob_z) join targets CtrlKnobs
pattern CtrlRow bodies CtrlKnobs dir (1,0,0) count 3 spacing ctrl_pitch

cylinder ValueSkirt radius (value_r+2 mm) height skirt_h base (value_x,knob_y,knob_z) new -> body ValueKnob
cylinder ValueCap radius value_r height (knob_h-4 mm) base (value_x,knob_y,knob_z) join targets ValueKnob
box ValuePointer size pointer_w value_r (2 mm) corner (value_x-pointer_w/2,knob_y,knob_z+knob_h-5 mm) cut targets ValueKnob
cylinder ValueBore radius shaft_r height (bore_depth-2 mm) base (value_x,knob_y,knob_z-1 mm) cut targets ValueKnob
box ValueFlat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) (bore_depth-2 mm) corner (value_x+shaft_flat,knob_y-shaft_r-0.1 mm,knob_z) join targets ValueKnob

cylinder VolumeCap radius vol_r height (knob_h-4 mm) base (vol_x,vol_y,knob_z) new -> body VolumeKnob
box VolumePointer size pointer_w vol_r (2 mm) corner (vol_x-pointer_w/2,vol_y,knob_z+knob_h-5 mm) cut targets VolumeKnob
cylinder VolumeBore radius shaft_r height (bore_depth-2 mm) base (vol_x,vol_y,knob_z-1 mm) cut targets VolumeKnob
box VolumeFlat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) (bore_depth-2 mm) corner (vol_x+shaft_flat,vol_y-shaft_r-0.1 mm,knob_z) join targets VolumeKnob

// Control PCB standoffs: four in the pad gaps, two between the button rows, two under the strip
param standoff_r = 3.5 mm
let so_h = pcb_bottom - wall
let so_z = pcb_bottom - insert_depth
let sx0 = pads_x + pad + pad_gap/2
let sx1 = sx0 + 2*pitch
let sy0 = pads_y + pad + pad_gap/2
let sy1 = sy0 + 2*pitch
let sy2 = (bank_y + btn_h + fx_y)/2
let sy3 = D - 20 mm
let sx2 = 50 mm
let sx3 = W - 50 mm
cylinder Standoff0 radius standoff_r height so_h base (sx0,sy0,wall) join targets Tray
cylinder Standoff1 radius standoff_r height so_h base (sx1,sy0,wall) join targets Tray
cylinder Standoff2 radius standoff_r height so_h base (sx0,sy1,wall) join targets Tray
cylinder Standoff3 radius standoff_r height so_h base (sx1,sy1,wall) join targets Tray
cylinder Standoff4 radius standoff_r height so_h base (sx2,sy2,wall) join targets Tray
cylinder Standoff5 radius standoff_r height so_h base (sx3,sy2,wall) join targets Tray
cylinder Standoff6 radius standoff_r height so_h base (sx2,sy3,wall) join targets Tray
cylinder Standoff7 radius standoff_r height so_h base (sx3,sy3,wall) join targets Tray
cylinder SoInsert0 radius insert_r height (insert_depth+1 mm) base (sx0,sy0,so_z) cut targets Tray
cylinder SoInsert1 radius insert_r height (insert_depth+1 mm) base (sx1,sy0,so_z) cut targets Tray
cylinder SoInsert2 radius insert_r height (insert_depth+1 mm) base (sx0,sy1,so_z) cut targets Tray
cylinder SoInsert3 radius insert_r height (insert_depth+1 mm) base (sx1,sy1,so_z) cut targets Tray
cylinder SoInsert4 radius insert_r height (insert_depth+1 mm) base (sx2,sy2,so_z) cut targets Tray
cylinder SoInsert5 radius insert_r height (insert_depth+1 mm) base (sx3,sy2,so_z) cut targets Tray
cylinder SoInsert6 radius insert_r height (insert_depth+1 mm) base (sx2,sy3,so_z) cut targets Tray
cylinder SoInsert7 radius insert_r height (insert_depth+1 mm) base (sx3,sy3,so_z) cut targets Tray

// Control PCB (reference): inside the bosses, M3 clearance at every standoff
param pcb_inset = 12 mm
sketch PcbSk on XY offset pcb_bottom {
  rect rounded board = (pcb_inset,pcb_inset) (W-pcb_inset,D-pcb_inset) radius 3
  fix board.bottom board.right board.top board.left board.arc1 board.arc2 board.arc3 board.arc4
}
extrude Pcb from PcbSk distance pcb_t new -> body ControlPcb
cylinder PcbHole0 radius 1.7 mm height (pcb_t+2 mm) base (sx0,sy0,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole1 radius 1.7 mm height (pcb_t+2 mm) base (sx1,sy0,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole2 radius 1.7 mm height (pcb_t+2 mm) base (sx0,sy1,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole3 radius 1.7 mm height (pcb_t+2 mm) base (sx1,sy1,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole4 radius 1.7 mm height (pcb_t+2 mm) base (sx2,sy2,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole5 radius 1.7 mm height (pcb_t+2 mm) base (sx3,sy2,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole6 radius 1.7 mm height (pcb_t+2 mm) base (sx2,sy3,pcb_bottom-1 mm) cut targets ControlPcb
cylinder PcbHole7 radius 1.7 mm height (pcb_t+2 mm) base (sx3,sy3,pcb_bottom-1 mm) cut targets ControlPcb

// Rear I/O through the back wall, low enough to clear the PCB: OUT L/R and IN L/R 1/4" jacks
// (3/8" bushings), Neutrik NCJ6FI-S combo (24 mm D cutout, drill its two M3 holes from the
// datasheet), USB-C breakout and a 5.5 x 2.1 mm DC jack (M8 thread)
param jack_r = 4.85 mm
param jack_pitch = 18 mm
param combo_r = 12 mm
param io_z = 16 mm
let cut_len = wall + 2 mm
cylinder OutL radius jack_r height cut_len base (W-20 mm,D+1 mm,io_z) axis (0,-1,0) cut targets Tray
pattern RearJacks features OutL dir (-1,0,0) count 4 spacing jack_pitch
cylinder Combo radius combo_r height cut_len base (W-20 mm-4*jack_pitch-8 mm,D+1 mm,io_z) axis (0,-1,0) cut targets Tray
param usb_w = 9.5 mm
param usb_h = 3.5 mm
box UsbC size usb_w cut_len usb_h corner (16 mm,D-wall-1 mm,io_z-usb_h/2) cut targets Tray
cylinder DcIn radius 4 mm height cut_len base (36 mm,D+1 mm,io_z) axis (0,-1,0) cut targets Tray

// Front edge: 3.5 mm headphone jack (M6 thread) on the left, full-size SD slot on the right
param phones_r = 3.1 mm
cylinder Phones radius phones_r height cut_len base (24 mm,-1 mm,io_z) axis (0,1,0) cut targets Tray
param sd_w = 26 mm
param sd_h = 3 mm
box SdSlot size sd_w cut_len sd_h corner (W-24 mm-sd_w,-1 mm,io_z-sd_h/2) cut targets Tray

// Rubber feet (reference: 14 x 3 mm adhesive bumpers) in the four corners
param foot_r = 7 mm
param foot_h = 3 mm
param foot_inset = 18 mm
cylinder Foot radius foot_r height foot_h base (foot_inset,foot_inset,-foot_h) new -> body Feet
pattern FeetGrid bodies Feet dir (1,0,0) count 2 spacing (W-2*foot_inset) dir2 (0,1,0) count2 2 spacing2 (D-2*foot_inset)
