// 132. 3.5 in floppy disk at true size, printable and working: a microSD card keeper with a sprung
// sliding shutter, a write-protect slider, the hub recess and two press-fit halves.
//
// The disk lies label side up: X across (0 at the left edge, 90 mm), Y from the label edge (y = 0)
// to the shutter edge (y = 94 mm), Z through it (0 on the hub side, 3.3 mm on the label side). The
// shell is two 1.65 mm halves; the corner at (90, 94) is chamfered, as on the real disk, so it only
// goes in one way.
//
// Inside, where the disk would be, a microSD card lies in a pocket behind the shutter edge, under
// the head window. The window is 9 mm wide, narrower than the 11 mm card, so the card shows but
// can't fall out. Slide the shutter right (`shutter` up to `travel`) and two things line up: its
// face windows with the head window, and a cutout in its spine with the card slot in the edge. Push
// the card out through the edge with a fingertip in the window; push a card in the same way.
//
// The shutter is a U of 0.4 mm plates and a spine, sitting in 0.45 mm recesses so it is flush. A
// lip on each plate's inner edge runs in a groove in the recess, which holds it on. A tab on the
// spine reaches into the shell through a slot in the edge and stands against the tip of a printed
// leaf spring, a 0.7 x 1 x 41 mm cantilever whose root drops into a socket. Opening the shutter
// bends the leaf 14 mm at its tip (about 0.9 % strain), and the leaf pushes it shut again. The slot's
// ends are the shutter's stops.
//
// The write-protect slider runs in a channel at the (0, 0) corner, its knob through a slot in the
// hub side. Over the window it's protected, off it writable; it is held by friction. The hole at the
// other corner marks the disk high density. The hub side has the 25.6 mm hub recess with its
// spindle and drive-pin holes, and the label side a 0.2 mm label recess.
//
// Printing, PLA (PETG for the spring if you have it), 0.12 mm layers, 0.4 mm nozzle:
//   Bottom   hub side down          Top      label side down
//   Shutter  on its spine           Spring   on its side, the 1 mm face down
//   Slider   knob up
// The halves press together on four 1.9 mm pegs; put the spring, slider and a card in first, slide
// the shutter on from the open end of its recess, then press the halves shut (a drop of CA if loose).
design FloppyDisk units mm

// Shell
param W = 90 mm
param L = 94 mm
param T = 3.3 mm
param corner_r = 1.5 mm
param chamfer = 3.6 mm
param fit = 0.1 mm

// Shutter
param s_x0 = 20 mm             // closed shutter's left end
param s_len = 50 mm
param s_depth = 29.5 mm        // how far the plates reach down the faces
param plate_t = 0.4 mm
param travel = 14 mm
param shutter = 0 mm           // 0 closed, travel open

// Head window and card
param win_x = 38 mm
param win_w = 9 mm
param card_w = 11 mm
param card_l = 15 mm
param card_t = 0.8 mm

// Inner cavities (the spring, the slider channel) between z = cav_lo and cav_hi
param cav_lo = 1.05 mm
param cav_hi = 2.25 mm

// Hub
param hub_x = 45 mm
param hub_y = 43 mm
param hub_d = 25.6 mm

let half = T/2
let rec = plate_t + 0.05 mm                 // face recess depth
let rec_x0 = s_x0 - 0.3 mm
let rec_x1 = s_x0 + s_len + travel + 0.3 mm
let rec_y0 = L - s_depth - 0.5 mm
let edge_y = L - 0.5 mm                     // shell's edge under the spine
let card_x = win_x + win_w/2                // card centre
let tab_x = s_x0 + s_len - 3 mm             // tab's left face, closed
let leaf_x = tab_x + 2.1 mm                 // leaf's left face, 0.1 mm off the tab
let wp_x = 3 mm
let wp_y = 3.1 mm

// ==== Shell: two halves, each cut made in the halves it reaches
sketch Outline on XY {
  rect rounded o = (0,0) (W,L) radius corner_r
  fix o
}
extrude BottomBlock from Outline distance half new -> body Bottom
extrude TopBlock from Outline distance half offset half new -> body Top
sketch ChamferSk on XY offset (-1 mm) {
  polyline closed c = (W - chamfer, L + 1 mm) (W + 1 mm, L + 1 mm) (W + 1 mm, L - chamfer - 1 mm) (W, L - chamfer) (W - chamfer, L)
  fix c
}
extrude Chamfer from ChamferSk distance (T + 2 mm) cut targets Top, Bottom

// Shutter recesses: both faces and the edge, with the lips' grooves
box RecessBot size (rec_x1 - rec_x0) (L - rec_y0 + 1 mm) (rec + 1 mm) corner (rec_x0, rec_y0, -1 mm) cut targets Bottom
box RecessTop size (rec_x1 - rec_x0) (L - rec_y0 + 1 mm) (rec + 1 mm) corner (rec_x0, rec_y0, T - rec) cut targets Top
box RecessEdge size (rec_x1 - rec_x0) 2 mm (T + 2 mm) corner (rec_x0, edge_y, -1 mm) cut targets Top, Bottom
box GrooveBot size (rec_x1 - rec_x0) 1.2 mm (rec + 0.5 mm) corner (rec_x0, rec_y0 + 0.3 mm, -0.1 mm) cut targets Bottom
box GrooveTop size (rec_x1 - rec_x0) 1.2 mm (rec + 0.5 mm) corner (rec_x0, rec_y0 + 0.3 mm, T - rec - 0.4 mm) cut targets Top

// Head window, through; the card pocket behind the edge under it
box HeadWindow size win_w 23 mm (T + 2 mm) corner (win_x, L - 27 mm, -1 mm) cut targets Top, Bottom
box CardPocket size (card_w + 0.2 mm) (card_l + 2 mm) (card_t + 0.2 mm) corner (card_x - card_w/2 - 0.1 mm, L - card_l - 1 mm, half - card_t/2 - 0.1 mm) cut targets Top, Bottom

// Spring bay: the leaf's sweep, the root's socket, and the tab's slot through the edge
box SpringBay size 21.6 mm 40.9 mm (cav_hi - cav_lo) corner (tab_x - 3 mm, L - 1.5 mm - 40.9 mm, cav_lo) cut targets Top, Bottom
box RootSocket size 3.2 mm 3.4 mm (cav_hi - cav_lo) corner (leaf_x - 0.1 mm, L - 1.5 mm - 40.9 mm - 3.3 mm, cav_lo) cut targets Top, Bottom
box TabSlot size (2 mm + travel + 0.4 mm) 1.6 mm (cav_hi - cav_lo) corner (tab_x - 0.2 mm, L - 1.6 mm, cav_lo) cut targets Top, Bottom

// Write-protect: window through both faces, channel inside, knob slot in the hub side; HD hole
box WpWindow size 3.8 mm 4 mm (T + 2 mm) corner (wp_x + 0.6 mm, wp_y + 0.6 mm, -1 mm) cut targets Top, Bottom
box WpChannel size (5 mm + 2*fit) (10 mm + 0.2 mm) (cav_hi - cav_lo) corner (wp_x - fit, wp_y - fit, cav_lo) cut targets Top, Bottom
box WpKnobSlot size 1.9 mm (1.9 mm + 4.8 mm) (cav_lo + 1 mm) corner (wp_x + 1.55 mm, wp_y + 1.55 mm, -1 mm) cut targets Top, Bottom
box HdHole size 3.8 mm 4 mm (T + 2 mm) corner (W - wp_x - 4.4 mm, wp_y + 0.6 mm, -1 mm) cut targets Top, Bottom

// Hub recess on the hub side, with the spindle hole and the drive-pin hole
cylinder HubRecess diameter hub_d height 1.6 mm base (hub_x, hub_y, -1 mm) axis Z cut targets Bottom
box Spindle size 4 mm 4 mm 1.1 mm corner (hub_x - 2 mm, hub_y - 2 mm, 0.5 mm) cut targets Bottom
box DriveHole size 2.4 mm 4 mm 1.1 mm corner (hub_x + 6.3 mm, hub_y - 2 mm, 0.5 mm) cut targets Bottom

// Label recess on the label side
sketch LabelSk on XY offset (T - 0.2 mm) {
  rect rounded l = (10 mm, 1.5 mm) (W - 10 mm, 58 mm) radius 2 mm
  fix l
}
extrude Label from LabelSk distance 1 mm cut targets Top

// Pegs on the bottom half, sockets in the top half (0.03 mm interference)
cylinder Peg0 diameter 1.9 mm height 1.4 mm base (6 mm, 30 mm, half - 0.5 mm) axis Z join targets Bottom
cylinder Socket0 diameter 1.95 mm height 1.1 mm base (6 mm, 30 mm, half - 0.1 mm) axis Z cut targets Top
cylinder Peg1 diameter 1.9 mm height 1.4 mm base (84 mm, 30 mm, half - 0.5 mm) axis Z join targets Bottom
cylinder Socket1 diameter 1.95 mm height 1.1 mm base (84 mm, 30 mm, half - 0.1 mm) axis Z cut targets Top
cylinder Peg2 diameter 1.9 mm height 1.4 mm base (6 mm, 86 mm, half - 0.5 mm) axis Z join targets Bottom
cylinder Socket2 diameter 1.95 mm height 1.1 mm base (6 mm, 86 mm, half - 0.1 mm) axis Z cut targets Top
cylinder Peg3 diameter 1.9 mm height 1.4 mm base (60 mm, 20 mm, half - 0.5 mm) axis Z join targets Bottom
cylinder Socket3 diameter 1.95 mm height 1.1 mm base (60 mm, 20 mm, half - 0.1 mm) axis Z cut targets Top

// ==== Shutter: two plates and a spine, a lip on each plate, the tab on the spine
box PlateBot size s_len (L - rec_y0 - 0.5 mm) plate_t corner (s_x0, rec_y0 + 0.5 mm, 0) new -> body Shutter
box Spine size s_len 0.4 mm T corner (s_x0, L - 0.4 mm, 0) join targets Shutter
box PlateTop size s_len (L - rec_y0 - 0.5 mm) plate_t corner (s_x0, rec_y0 + 0.5 mm, T - plate_t) join targets Shutter
box LipBot size s_len 0.8 mm 0.4 mm corner (s_x0, rec_y0 + 0.5 mm, plate_t - 0.05 mm) join targets Shutter
box LipTop size s_len 0.8 mm 0.4 mm corner (s_x0, rec_y0 + 0.5 mm, T - plate_t - 0.35 mm) join targets Shutter
box Tab size 2 mm 3.05 mm 0.8 mm corner (tab_x, L - 3.4 mm, half - 0.4 mm) join targets Shutter
box ShutterWin size win_w 23 mm (T + 2 mm) corner (win_x - travel, L - 27 mm, -1 mm) cut targets Shutter
box SpineWin size (card_w + 0.4 mm) 2 mm (card_t + 0.5 mm) corner (card_x - card_w/2 - 0.2 mm - travel, L - 1 mm, half - card_t/2 - 0.25 mm) cut targets Shutter
move OpenShutter bodies Shutter to (shutter, 0, 0)

// ==== Leaf spring: a root block and a 0.7 mm leaf reaching up beside the tab
box Root size 3 mm 3.3 mm (cav_hi - cav_lo - 0.1 mm) corner (leaf_x, L - 1.5 mm - 40.9 mm - 3.2 mm, cav_lo + 0.05 mm) new -> body Spring
box Leaf size 0.7 mm 40.85 mm 1 mm corner (leaf_x, L - 1.5 mm - 40.9 mm - 0.05 mm, half - 0.5 mm) join targets Spring

// ==== Write-protect slider (as shown, over the window: protected)
box SliderBlock size 5 mm 5 mm (cav_hi - cav_lo - 0.1 mm) corner (wp_x, wp_y, cav_lo + 0.05 mm) new -> body Slider
box SliderKnob size 1.5 mm 1.5 mm (cav_lo - 0.2 mm) corner (wp_x + 1.75 mm, wp_y + 1.75 mm, 0.3 mm) join targets Slider

// ==== The card, for checking fit (not printed)
box RefCard size card_w card_l card_t corner (card_x - card_w/2, L - 0.8 mm - card_l, half - card_t/2) new -> body RefCard

// ==== Fits. The engine's clearance query reads 0 mm for a part lying in a pocket that spans both
// halves (it names a point in the pocket), so the parts inside the shell are checked with the print
// recipe's interference request instead: no part overlaps another. The designed gaps are 0.05 mm on
// the shutter's faces, 0.1 mm at its spine, lips and tab, 0.05-0.1 mm round the spring and the slider,
// 0.1-0.2 mm round the card.
check Spring clearance Shutter >= 0.05 mm
check RefCard clearance Shutter >= 0.3 mm

color Bottom, Top "#2a2c30"
color Shutter "#c7ccd2"
color Spring "#e0b040"
color Slider "#2a2c30"
color RefCard "#1b4f9c"
material Bottom, Top, Shutter, Spring, Slider PLA
