// 114. Eurorack front panel generator, with knobs and a printable case rail section.
//
// The panel lies on the bed as it prints: X across its width, Y up its 128.5 mm height (the 3U
// Eurorack panel height), Z out of its face, back face at z = 0. Widths follow the spec: one HP is
// 5.08 mm and a panel is hp*5.08 mm less a little play (`play`), so neighbours never bind. The
// mounting slots sit 3 mm in from the top and bottom edges and 7.5 mm in from the left edge; a
// panel of 10 HP or more gets a second pair (hp - 3) HP further right, matching the rail holes.
//
// Controls (set the counts below): a column of 9 mm pots (Alpha 9 mm with M7 bushing, 7.2 mm
// holes), an LED (3 mm, 3.1 mm hole) and a grid of 3.5 mm jacks (Thonkiconn PJ398SM, 6 mm holes)
// in one or two columns. All holes are cut from one sketch. Two stiffening ribs run up the back,
// clear of the rails and of every control. The knobs press onto 6 mm knurled pot shafts: a 6.0 mm
// bore with a slit so they grip, a skirt and a pointer line.
//
// CaseRail is a printable stand-in for an aluminium Eurorack rail, 42 HP long (fits a 256 mm bed):
// a channel with lips holding M3 square nuts (5.5 x 1.8 mm) that slide to any HP, a flange with two M4 holes to screw it
// into a case side, and the panel face at the front. Print two per row; the rails sit 122.5 mm apart
// (hole to hole) so the panel's slots line up.
//
// Print: the panel face down (z = 0 is the bed after the flip in the recipe; the ribs then point
// up), the knobs skirt down, the rails on their flange side. PLA, 0.2 mm layers, no supports.
// BOM: Alpha 9 mm pots, Thonkiconn PJ398SM jacks, a 3 mm LED with bezel or bare, M3x6 screws and
// M3 square nuts (or the case's own sliding nuts).
design EurorackPanel units mm

param hp = 8                  // panel width in HP (1 HP = 5.08 mm)
param play = 0.3 mm
param panel_t = 2 mm
param pot_count = 2           // 9 mm pots in a column near the top
param jack_rows = 3
param jack_cols = 2           // 1 or 2 columns of jacks
param rib_h = 4 mm
param rail_hp = 42

let w = hp*5.08 mm - play
let h = 128.5 mm
let slot_x = 7.5 mm
let pot_y0 = 100 mm
let pot_pitch = 24 mm
let led_y = pot_y0 - pot_count*pot_pitch + 12 mm
let jack_y0 = 18 mm
let jack_pitch = 15 mm
let jack_dx = (jack_cols - 1)*w/4

// ==== Panel
box PanelPlate size w h panel_t corner (0, 0, 0) new -> body Panel
sketch PanelHoles on XY offset (-1 mm) {
  slot top_l = centers (slot_x - 1.2 mm, h - 3 mm) (slot_x + 1.2 mm, h - 3 mm) width 3.2 mm
  slot bot_l = centers (slot_x - 1.2 mm, 3 mm) (slot_x + 1.2 mm, 3 mm) width 3.2 mm
  circle led = (w/2, led_y) diameter 3.1 mm
  fix top_l bot_l led
}
extrude PanelHoleCut from PanelHoles distance (panel_t + 2 mm) cut targets Panel
if hp >= 10 {
  sketch PanelHoles2 on XY offset (-1 mm) {
    slot top_r = centers (slot_x + (hp - 3)*5.08 mm - 1.2 mm, h - 3 mm) (slot_x + (hp - 3)*5.08 mm + 1.2 mm, h - 3 mm) width 3.2 mm
    slot bot_r = centers (slot_x + (hp - 3)*5.08 mm - 1.2 mm, 3 mm) (slot_x + (hp - 3)*5.08 mm + 1.2 mm, 3 mm) width 3.2 mm
    fix top_r bot_r
  }
  extrude PanelHoleCut2 from PanelHoles2 distance (panel_t + 2 mm) cut targets Panel
}
for k from 0 to 3 {
  if k < pot_count {
    cylinder PotHole{k} diameter 7.2 mm height (panel_t + 2 mm) base (w/2, pot_y0 - k*pot_pitch, -1 mm) axis Z cut targets Panel
  }
}
for r from 0 to 5 {
  if r < jack_rows {
    for c from 0 to 1 {
      if c < jack_cols {
        cylinder JackHole{r}{c} diameter 6 mm height (panel_t + 2 mm) base (w/2 - jack_dx + c*2*jack_dx, jack_y0 + r*jack_pitch, -1 mm) axis Z cut targets Panel
      }
    }
  }
}
// Stiffening ribs on the back, between the rail zones and clear of the controls
box RibL size 1.6 mm (h - 24 mm) (rib_h + 0.3 mm) corner (1.2 mm, 12 mm, -rib_h) join targets Panel
box RibR size 1.6 mm (h - 24 mm) (rib_h + 0.3 mm) corner (w - 2.8 mm, 12 mm, -rib_h) join targets Panel

// ==== Knobs: one per pot, standing on the face above its hole
for k from 0 to 3 {
  if k < pot_count {
    cylinder KnobSkirt{k} diameter 16 mm height 2 mm base (w/2, pot_y0 - k*pot_pitch, panel_t + 4 mm) axis Z new -> body Knob{k}
    cylinder KnobBody{k} diameter 12 mm height 12 mm base (w/2, pot_y0 - k*pot_pitch, panel_t + 5.8 mm) axis Z join targets Knob{k}
    cylinder KnobBore{k} diameter 6 mm height 10 mm base (w/2, pot_y0 - k*pot_pitch, panel_t + 3.9 mm) axis Z cut targets Knob{k}
    box KnobSlit{k} size 0.8 mm 8 mm 8 mm corner (w/2 - 0.4 mm, pot_y0 - k*pot_pitch - 4 mm, panel_t + 3.9 mm) cut targets Knob{k}
    box KnobPointer{k} size 1 mm 5 mm 0.6 mm corner (w/2 - 0.5 mm, pot_y0 - k*pot_pitch + 0.8 mm, panel_t + 17.5 mm) cut targets Knob{k}
  }
}

// ==== Case rail section, beside the panel: X along its length, profile in YZ
let rail_l = rail_hp*5.08 mm
let rx = w + 20 mm
sketch RailProfile on YZ offset rx {
  polyline closed p = (0, 0) (22 mm, 0) (22 mm, 3 mm) (13 mm, 3 mm) (13 mm, 10 mm) (9.7 mm, 10 mm) (9.7 mm, 8 mm) (10.9 mm, 8 mm) (10.9 mm, 5 mm) (5.1 mm, 5 mm) (5.1 mm, 8 mm) (6.3 mm, 8 mm) (6.3 mm, 10 mm) (3 mm, 10 mm) (3 mm, 13.5 mm) (0, 13.5 mm)
  fix p
}
extrude RailBody from RailProfile distance rail_l new -> body CaseRail
for k from 0 to 1 {
  cylinder RailScrew{k} diameter 4.4 mm height 5 mm base (rx + rail_l*(0.2 + 0.6*k), 17.5 mm, -1 mm) axis Z cut targets CaseRail
}

// ==== Finish
color Panel "#c9ccd0"
color CaseRail "#9aa0a6"
for k from 0 to 3 {
  if k < pot_count {
    color Knob{k} "#202124"
  }
}
material Panel, CaseRail PLA
