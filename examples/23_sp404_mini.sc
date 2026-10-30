// 23. SP-404 mini: a pocket sampler built around one 100 x 100 mm PCB, with the pads in the copper.
//
// The board is the cheapest JLCPCB size class. Each pad is an interdigitated electrode pair on the
// top copper (ENIG); one Velostat sheet over the field and a TPU pad mat on top turn every pair into
// a force sensor, read through a 16-channel mux for velocity. Top side, hand soldered: 4 tactile
// switches (6x6x5 mm), an EC11 encoder, 3 x 9 mm pots and a 0.96" 128x64 OLED on a header. Bottom
// side, machine assembled: ESP32-S3-WROOM-1-N16R8, ES8388 codec, 2 x PJ-320A jacks (phones, line in)
// and USB-C on the left edge, a microSD socket on the right edge.
//
// Board coordinates: origin at the PCB's front left corner, X right, Y to the rear, Z up from the
// underside of the tray. Stack: floor 0-2.5, bottom-side parts, PCB 9-10.6, Velostat 0.1 mm, pad
// mat web, tactile plungers at 15.6, cap flanges 15.6-16.6, panel 16.6-19.1. Four M3 x 14
// countersunk screws pass through the panel, its bosses and the PCB into inserts in the tray, so
// the board is clamped between the bosses.
//
// Print: Tray floor down; Panel top down (the pad grid and bosses grow from the underside); PadMat
// in TPU 95A, web down; caps and knobs flange or base down. PCB, Velostat, Oled and BottomParts
// are reference bodies.
design SP404Mini units mm

// Board and case outline; the rear has extra room for the OLED module's overhang
param B = 100 mm
param clr = 0.5 mm
param wall = 2.5 mm
param rear_extra = 4 mm
param case_r = 5 mm
let x0 = -(clr + wall)
let y0 = -(clr + wall)
let x1 = B + clr + wall
let y1 = B + clr + rear_extra + wall

// Vertical stack
param under_h = 6.5 mm
param pcb_t = 1.6 mm
param velo_t = 0.1 mm
param web_t = 0.8 mm
param switch_h = 5 mm
param flange_t = 1 mm
param panel_t = 2.5 mm
let pcb_bottom = wall + under_h
let pcb_top = pcb_bottom + pcb_t
let velo_top = pcb_top + velo_t
let web_top = velo_top + web_t
let panel_bottom = pcb_top + switch_h + flange_t
let panel_top = panel_bottom + panel_t

// Tray, open at the top; walls reach the panel
sketch TraySk on XY {
  rect rounded outline = (x0,y0) (x1,y1) radius case_r
  fix outline.bottom outline.right outline.top outline.left outline.arc1 outline.arc2 outline.arc3 outline.arc4
}
extrude TrayBlock from TraySk distance panel_bottom -> body Tray
shell TrayCavity faces ((x0+x1)/2,(y0+y1)/2,panel_bottom) thickness wall inside body Tray

// PCB mounting: M3 holes 3.5 mm in from each board corner; tray bosses carry the board on inserts
param hole_in = 3.5 mm
param boss_r = 3.5 mm
param insert_r = 2 mm
param insert_depth = 5 mm
let hx1 = B - hole_in
let hy1 = B - hole_in
cylinder Boss0 radius boss_r height under_h base (hole_in,hole_in,wall) join targets Tray
cylinder Boss1 radius boss_r height under_h base (hx1,hole_in,wall) join targets Tray
cylinder Boss2 radius boss_r height under_h base (hole_in,hy1,wall) join targets Tray
cylinder Boss3 radius boss_r height under_h base (hx1,hy1,wall) join targets Tray
cylinder Insert0 radius insert_r height (insert_depth+1 mm) base (hole_in,hole_in,pcb_bottom-insert_depth) cut targets Tray
cylinder Insert1 radius insert_r height (insert_depth+1 mm) base (hx1,hole_in,pcb_bottom-insert_depth) cut targets Tray
cylinder Insert2 radius insert_r height (insert_depth+1 mm) base (hole_in,hy1,pcb_bottom-insert_depth) cut targets Tray
cylinder Insert3 radius insert_r height (insert_depth+1 mm) base (hx1,hy1,pcb_bottom-insert_depth) cut targets Tray

// Edge openings: phones and line in (PJ-320A) and USB-C on the left wall, microSD on the right
param jack_r = 3.3 mm
param jack_drop = 2.5 mm
param phones_y = 84 mm
param line_y = 68 mm
param usb_y = 40 mm
param usb_w = 9.5 mm
param usb_h = 3.6 mm
param sd_y = 40 mm
param sd_w = 13 mm
param sd_h = 2.5 mm
let jack_z = pcb_bottom - jack_drop
let cut_len = wall + 2 mm
cylinder PhonesOpening radius jack_r height cut_len base (x0-1 mm,phones_y,jack_z) axis (1,0,0) cut targets Tray
cylinder LineOpening radius jack_r height cut_len base (x0-1 mm,line_y,jack_z) axis (1,0,0) cut targets Tray
box UsbOpening size cut_len usb_w usb_h corner (x0-1 mm,usb_y-usb_w/2,pcb_bottom-1.6 mm-usb_h/2) cut targets Tray
box SdOpening size cut_len sd_w sd_h corner (x1-wall-1 mm,sd_y-sd_w/2,pcb_bottom-0.9 mm-sd_h/2) cut targets Tray

// PCB (reference) with the four mounting holes
box Pcb size B B pcb_t corner (0,0,pcb_bottom) new -> body PCB
cylinder PcbHole0 radius 1.6 mm height (pcb_t+2 mm) base (hole_in,hole_in,pcb_bottom-1 mm) cut targets PCB
cylinder PcbHole1 radius 1.6 mm height (pcb_t+2 mm) base (hx1,hole_in,pcb_bottom-1 mm) cut targets PCB
cylinder PcbHole2 radius 1.6 mm height (pcb_t+2 mm) base (hole_in,hy1,pcb_bottom-1 mm) cut targets PCB
cylinder PcbHole3 radius 1.6 mm height (pcb_t+2 mm) base (hx1,hy1,pcb_bottom-1 mm) cut targets PCB

// Bottom-side parts (reference) to check the openings: module, jacks, USB-C, microSD
box Esp32S3 size 18 mm 25.5 mm 3.1 mm corner (55 mm,15 mm,pcb_bottom-3.1 mm) new -> body BottomParts
box PhonesJack size 12 mm 6 mm (2*jack_drop) corner (0,phones_y-3 mm,pcb_bottom-2*jack_drop) join targets BottomParts
box LineJack size 12 mm 6 mm (2*jack_drop) corner (0,line_y-3 mm,pcb_bottom-2*jack_drop) join targets BottomParts
box UsbC size 7.4 mm 9 mm 3.2 mm corner (0,usb_y-4.5 mm,pcb_bottom-3.2 mm) join targets BottomParts
box MicroSd size 14 mm 15 mm 1.8 mm corner (B-14 mm,sd_y-7.5 mm,pcb_bottom-1.8 mm) join targets BottomParts

// Pad field: 4 x 4 copper pads, 14.5 mm square on a 17 mm pitch, front right of the board
param pad = 14.5 mm
param pad_gap = 2.5 mm
param pad_r = 2 mm
param pad_rise = 1.5 mm
param pads_x = 27 mm
param pads_y = 7 mm
param web_margin = 1 mm
param block_margin = 2 mm
param fit = 0.3 mm
let pitch = pad + pad_gap
let field = 4*pad + 3*pad_gap
box Velo size (field+2*web_margin) (field+2*web_margin) velo_t corner (pads_x-web_margin,pads_y-web_margin,pcb_top) new -> body Velostat

// Pad mat (TPU): one web over the Velostat, sixteen pads standing through the panel
sketch WebSk on XY offset velo_top {
  rect rounded web = (pads_x-web_margin,pads_y-web_margin) (pads_x+field+web_margin,pads_y+field+web_margin) radius pad_r
  fix web.bottom web.right web.top web.left web.arc1 web.arc2 web.arc3 web.arc4
}
extrude Web from WebSk distance web_t new -> body PadMat
sketch PadSk on XY offset velo_top {
  rect rounded p = (pads_x,pads_y) (pads_x+pad,pads_y+pad) radius pad_r
  fix p.bottom p.right p.top p.left p.arc1 p.arc2 p.arc3 p.arc4
}
extrude Pad from PadSk distance (panel_top+pad_rise-velo_top) join targets PadMat
pattern PadGrid features Pad dir (1,0,0) count 4 spacing pitch dir2 (0,1,0) count2 4 spacing2 pitch

// Panel: plate on the tray walls, a pad grid block that holds the web down, corner bosses
sketch PanelSk on XY offset panel_bottom {
  rect rounded outline = (x0,y0) (x1,y1) radius case_r
  fix outline.bottom outline.right outline.top outline.left outline.arc1 outline.arc2 outline.arc3 outline.arc4
}
extrude PanelPlate from PanelSk distance panel_t new -> body Panel
chamfer TopEdge edges ((x0+x1)/2,y0,panel_top) distance 1 mm body Panel
sketch BlockSk on XY offset web_top {
  rect rounded blk = (pads_x-block_margin,pads_y-block_margin) (pads_x+field+block_margin,pads_y+field+block_margin) radius (pad_r+block_margin)
  fix blk.bottom blk.right blk.top blk.left blk.arc1 blk.arc2 blk.arc3 blk.arc4
}
extrude PadBlock from BlockSk distance (panel_bottom-web_top) join targets Panel
sketch PadHoleSk on XY offset panel_top {
  rect rounded h = (pads_x-fit,pads_y-fit) (pads_x+pad+fit,pads_y+pad+fit) radius (pad_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude PadHole from PadHoleSk distance (panel_top-web_top+0.5 mm) negative cut targets Panel
pattern PadHoles features PadHole dir (1,0,0) count 4 spacing pitch dir2 (0,1,0) count2 4 spacing2 pitch
param pboss_r = 3 mm
let pboss_h = panel_bottom - pcb_top
cylinder PBoss0 radius pboss_r height pboss_h base (hole_in,hole_in,pcb_top) join targets Panel
cylinder PBoss1 radius pboss_r height pboss_h base (hx1,hole_in,pcb_top) join targets Panel
cylinder PBoss2 radius pboss_r height pboss_h base (hole_in,hy1,pcb_top) join targets Panel
cylinder PBoss3 radius pboss_r height pboss_h base (hx1,hy1,pcb_top) join targets Panel
// Countersunk M3 clearance through the panel and its bosses
param sink = 1.55 mm
let bore_h = panel_top - pcb_top + 1 mm
cylinder Screw0 radius 1.7 mm height bore_h base (hole_in,hole_in,pcb_top-0.2 mm) cut targets Panel
cylinder Screw1 radius 1.7 mm height bore_h base (hx1,hole_in,pcb_top-0.2 mm) cut targets Panel
cylinder Screw2 radius 1.7 mm height bore_h base (hole_in,hy1,pcb_top-0.2 mm) cut targets Panel
cylinder Screw3 radius 1.7 mm height bore_h base (hx1,hy1,pcb_top-0.2 mm) cut targets Panel
chamfer Countersinks edges (hole_in+1.7 mm,hole_in,panel_top) (hx1+1.7 mm,hole_in,panel_top) (hole_in+1.7 mm,hy1,panel_top) (hx1+1.7 mm,hy1,panel_top) distance sink body Panel

// Buttons: REC, SHIFT, BANK, FX in a column left of the pads, one per pad row
param btn_w = 15 mm
param btn_h = 9 mm
param btn_r = 2 mm
param btn_lip = 1.5 mm
param btn_x = 6.5 mm
let btn_y = pads_y + pad/2 - btn_h/2
let cap_h = panel_t + pad_rise
sketch BtnHoleSk on XY offset panel_top {
  rect rounded h = (btn_x-fit,btn_y-fit) (btn_x+btn_w+fit,btn_y+btn_h+fit) radius (btn_r+fit)
  fix h.bottom h.right h.top h.left h.arc1 h.arc2 h.arc3 h.arc4
}
extrude BtnHole from BtnHoleSk distance (panel_t+1 mm) negative cut targets Panel
pattern BtnHoles features BtnHole dir (0,1,0) count 4 spacing pitch
sketch BtnFlangeSk on XY offset (panel_bottom-flange_t) {
  rect rounded f = (btn_x-btn_lip,btn_y-btn_lip) (btn_x+btn_w+btn_lip,btn_y+btn_h+btn_lip) radius (btn_r+btn_lip)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude BtnFlange from BtnFlangeSk distance flange_t new -> body Buttons
sketch BtnCapSk on XY offset panel_bottom {
  rect rounded c = (btn_x,btn_y) (btn_x+btn_w,btn_y+btn_h) radius btn_r
  fix c.bottom c.right c.top c.left c.arc1 c.arc2 c.arc3 c.arc4
}
extrude BtnCap from BtnCapSk distance cap_h join targets Buttons
pattern BtnColumn bodies Buttons dir (0,1,0) count 4 spacing pitch

// Control strip along the rear: VALUE encoder, VOLUME pot, OLED, CTRL 1 and CTRL 2 pots
param strip_y = 88 mm
param enc_x = 15 mm
param vol_x = 29 mm
param ctrl_x = 72 mm
param ctrl_pitch = 14 mm
param oled_x = 50 mm
param pot_body_h = 6.5 mm
param pot_body = 12.5 mm
param bushing_r = 3.6 mm
let pocket_d = max(0 mm, pot_body_h - (panel_bottom - pcb_top)) + 0.3 mm
box EncPocket size (pot_body+0.5 mm) (pot_body+0.5 mm) (pocket_d+0.1 mm) corner (enc_x-pot_body/2-0.25 mm,strip_y-pot_body/2-0.25 mm,panel_bottom-0.1 mm) cut targets Panel
box VolPocket size pot_body pot_body (pocket_d+0.1 mm) corner (vol_x-pot_body/2,strip_y-pot_body/2,panel_bottom-0.1 mm) cut targets Panel
box CtrlPocket size pot_body pot_body (pocket_d+0.1 mm) corner (ctrl_x-pot_body/2,strip_y-pot_body/2,panel_bottom-0.1 mm) cut targets Panel
pattern CtrlPockets features CtrlPocket dir (1,0,0) count 2 spacing ctrl_pitch
cylinder EncShaft radius bushing_r height (panel_t+2 mm) base (enc_x,strip_y,panel_bottom-1 mm) cut targets Panel
cylinder VolShaft radius bushing_r height (panel_t+2 mm) base (vol_x,strip_y,panel_bottom-1 mm) cut targets Panel
cylinder CtrlShaft radius bushing_r height (panel_t+2 mm) base (ctrl_x,strip_y,panel_bottom-1 mm) cut targets Panel
pattern CtrlShafts features CtrlShaft dir (1,0,0) count 2 spacing ctrl_pitch

// 0.96" OLED module (27.3 x 27.8 mm) on a 2.5 mm header; the window frames the active area
param oled_mod_w = 27.3 mm
param oled_mod_h = 27.8 mm
param oled_mod_y = 75 mm
param header_h = 2.5 mm
param glass_w = 26.7 mm
param glass_h = 19.3 mm
param win_w = 23 mm
param win_h = 12 mm
let oled_z = pcb_top + header_h
let glass_cy = oled_mod_y + oled_mod_h/2
box OledBoard size oled_mod_w oled_mod_h 1.2 mm corner (oled_x-oled_mod_w/2,oled_mod_y,oled_z) new -> body Oled
box OledGlass size glass_w glass_h 1.5 mm corner (oled_x-glass_w/2,glass_cy-glass_h/2,oled_z+1.2 mm) join targets Oled
box OledWindow size win_w win_h (panel_t+1 mm) corner (oled_x-win_w/2,glass_cy-win_h/2,panel_bottom-0.5 mm) cut targets Panel
sketch BezelSk on XY offset panel_top {
  rect rounded bz = (oled_x-win_w/2-2 mm,glass_cy-win_h/2-2 mm) (oled_x+win_w/2+2 mm,glass_cy+win_h/2+2 mm) radius 2
  fix bz.bottom bz.right bz.top bz.left bz.arc1 bz.arc2 bz.arc3 bz.arc4
}
extrude Bezel from BezelSk distance 0.6 mm negative cut targets Panel

// Knobs on 6 mm D shafts, 1 mm above the panel, pointer notch at 12 o'clock
param shaft_r = 3.1 mm
param shaft_flat = 1.5 mm
param knob_h = 10 mm
param enc_knob_r = 6.5 mm
param vol_knob_r = 5 mm
param ctrl_knob_r = 6 mm
param pointer_w = 1 mm
let knob_z = panel_top + 1 mm
let knob_bore = knob_h - 2 mm
cylinder EncCap radius enc_knob_r height knob_h base (enc_x,strip_y,knob_z) new -> body EncoderKnob
box EncPointer size pointer_w enc_knob_r 1.5 mm corner (enc_x-pointer_w/2,strip_y,knob_z+knob_h-1 mm) cut targets EncoderKnob
cylinder EncBore radius shaft_r height knob_bore base (enc_x,strip_y,knob_z-1 mm) cut targets EncoderKnob
box EncFlat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) (knob_bore-1 mm) corner (enc_x+shaft_flat,strip_y-shaft_r-0.1 mm,knob_z) join targets EncoderKnob
cylinder VolCap radius vol_knob_r height (knob_h-2 mm) base (vol_x,strip_y,knob_z) new -> body VolumeKnob
box VolPointer size pointer_w vol_knob_r 1.5 mm corner (vol_x-pointer_w/2,strip_y,knob_z+knob_h-3 mm) cut targets VolumeKnob
cylinder VolBore radius shaft_r height (knob_bore-2 mm) base (vol_x,strip_y,knob_z-1 mm) cut targets VolumeKnob
box VolFlat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) (knob_bore-3 mm) corner (vol_x+shaft_flat,strip_y-shaft_r-0.1 mm,knob_z) join targets VolumeKnob
cylinder CtrlCap radius ctrl_knob_r height knob_h base (ctrl_x,strip_y,knob_z) new -> body CtrlKnobs
box CtrlPointer size pointer_w ctrl_knob_r 1.5 mm corner (ctrl_x-pointer_w/2,strip_y,knob_z+knob_h-1 mm) cut targets CtrlKnobs
cylinder CtrlBore radius shaft_r height knob_bore base (ctrl_x,strip_y,knob_z-1 mm) cut targets CtrlKnobs
box CtrlFlat size (shaft_r-shaft_flat+0.2 mm) (2*shaft_r+0.2 mm) (knob_bore-1 mm) corner (ctrl_x+shaft_flat,strip_y-shaft_r-0.1 mm,knob_z) join targets CtrlKnobs
pattern CtrlRow bodies CtrlKnobs dir (1,0,0) count 2 spacing ctrl_pitch

// Rubber feet (reference: 8 x 2 mm adhesive bumpers)
param foot_r = 4 mm
param foot_h = 2 mm
param foot_in = 8 mm
cylinder Foot radius foot_r height foot_h base (x0+foot_in,y0+foot_in,-foot_h) new -> body Feet
pattern FeetGrid bodies Feet dir (1,0,0) count 2 spacing (x1-x0-2*foot_in) dir2 (0,1,0) count2 2 spacing2 (y1-y0-2*foot_in)

// The stand-ins for the board's parts (module, jacks, USB-C, card slot) are separate pieces of one body.
check BottomParts shells == 5
