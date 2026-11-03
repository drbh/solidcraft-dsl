// 152. Print-in-place flexi lizard: a chain of segments on captive pivots that wiggles off the bed in one piece.
//
// It prints flat, as modelled: Z is up from the bed, the head at +Y, the spine running down -Y,
// one segment every `pitch`. Nothing needs supports and nothing is assembled.
//
// Every joint is the same pivot about a vertical axis, so the lizard bends side to side the way a
// lizard walks. At each pivot the part in front (A) keeps the lower half of a round disc and the
// part behind (B) the upper half, `gap` apart, each cut back by `gap` round the other's half. A
// stem on A rises through a hole in B and ends in a 45 deg mushroom head in a countersink, both
// grown by `gap`, so B turns on A but can't lift off. The spine is one pivot between each pair of
// segments; each leg has a hip pivot on its torso segment and, with `leg_joints` = 2, a knee.
//
// `segments` (6 to 16) is the head plus the body and tail; the tail tapers to its last segment.
// The front legs hang on segment 2 and the hind legs on segment 4.
//
// Each segment's outline (body, spine bar, pivot discs, hip bars) is one sketch and one extrude:
// joining discs and bars on as separate solids made the engine retry its booleans on the coplanar
// top and bottom faces. The legs' joins come before the spine's pivot cuts, since a join after
// them brought cut material back on the torso segments (engine 0.28).
//
// Print in PLA or PETG at 0.2 mm layers, 0.4 mm line width, no supports. The B halves start
// `gap` above the A halves like a short bridge, so print the first layers above the joints slowly
// with cooling on. Flex every joint after printing to break it free; if they fuse raise `gap` to
// 0.4 mm, if they flop lower it to 0.3 mm.
design FlexiLizard units mm

param segments = 14            // head + body + tail, 6 to 16
param leg_joints = 2           // 1 hip only, 2 hip and knee
param gap = 0.35 mm            // every clearance in a joint
param pitch = 11 mm            // spine spacing
param T = 6 mm                 // thickness
param rt = 4.2 mm              // pivot disc radius
param rs = 1.5 mm              // stem radius
param hh = 1 mm                // mushroom head height (45 deg)
param wb = 5 mm                // spine and hip bars
param lw = 5 mm                // leg width
param torso_w = 10.5 mm        // half width of the torso segments

let t1 = (T - gap)/2           // top of the A halves; the B halves start gap above
let b = pitch/2 - gap/2 - 0.3 mm   // half length of a segment's body, clear of the next

// ==== Spine and legs, one segment at a time
for k from 0 to 15 {
  if k < segments {
    if k == 0 {
      sketch HeadSk on XY {
        ellipse e = (0, 3.5 mm) major (0, 12 mm) minor 8 mm
        polyline closed bar = (-wb/2, -pitch/2) (wb/2, -pitch/2) (wb/2, 0) (-wb/2, 0)
        circle dr = (0, -pitch/2) radius rt
        fix e bar dr
      }
      extrude Head from HeadSk distance T new -> body Seg0
      // Eyes and nostrils, pressed into the top
      cylinder EyeR radius 1.4 mm height 1 mm base (4 mm, 6 mm, T - 0.6 mm) axis Z cut targets Seg0
      cylinder EyeL radius 1.4 mm height 1 mm base (-4 mm, 6 mm, T - 0.6 mm) axis Z cut targets Seg0
      cylinder NoseR radius 0.6 mm height 1 mm base (1.6 mm, 10.5 mm, T - 0.5 mm) axis Z cut targets Seg0
      cylinder NoseL radius 0.6 mm height 1 mm base (-1.6 mm, 10.5 mm, T - 0.5 mm) axis Z cut targets Seg0
    }
    if k == 1 {
      sketch NeckSk on XY {
        ellipse e = (0, -pitch) major (7.5 mm, -pitch) minor b
        polyline closed bar = (-wb/2, -(k + 0.5)*pitch) (wb/2, -(k + 0.5)*pitch) (wb/2, -(k - 0.5)*pitch) (-wb/2, -(k - 0.5)*pitch)
        circle df = (0, -(k - 0.5)*pitch) radius rt
        circle dr = (0, -(k + 0.5)*pitch) radius rt
        fix e bar df dr
      }
      extrude Neck from NeckSk distance T new -> body Seg{k}
    }
    if (k - 2)*(k - 4) == 0 {
      sketch TorsoLegSk{k} on XY {
        ellipse e = (0, -k*pitch) major (torso_w, -k*pitch) minor b
        polyline closed bar = (-wb/2, -(k + 0.5)*pitch) (wb/2, -(k + 0.5)*pitch) (wb/2, -(k - 0.5)*pitch) (-wb/2, -(k - 0.5)*pitch)
        circle df = (0, -(k - 0.5)*pitch) radius rt
        circle dr = (0, -(k + 0.5)*pitch) radius rt
        polyline closed hip = (-(torso_w + 2 mm), -k*pitch - wb/2) ((torso_w + 2 mm), -k*pitch - wb/2) ((torso_w + 2 mm), -k*pitch + wb/2) (-(torso_w + 2 mm), -k*pitch + wb/2)
        circle hr = ((torso_w + 2 mm), -k*pitch) radius rt
        circle hl = (-(torso_w + 2 mm), -k*pitch) radius rt
        fix e bar df dr hip hr hl
      }
      extrude TorsoLeg{k} from TorsoLegSk{k} distance T new -> body Seg{k}
    }
    if k == 3 {
      sketch TorsoSk3 on XY {
        ellipse e = (0, -k*pitch) major (torso_w, -k*pitch) minor b
        polyline closed bar = (-wb/2, -(k + 0.5)*pitch) (wb/2, -(k + 0.5)*pitch) (wb/2, -(k - 0.5)*pitch) (-wb/2, -(k - 0.5)*pitch)
        circle df = (0, -(k - 0.5)*pitch) radius rt
        circle dr = (0, -(k + 0.5)*pitch) radius rt
        fix e bar df dr
      }
      extrude Torso3 from TorsoSk3 distance T new -> body Seg3
    }
    if k > 4 {
      if torso_w*(1 - (k - 4)/(segments - 3.5)) >= b + 0.2 mm {
        sketch TailWideSk{k} on XY {
          ellipse e = (0, -k*pitch) major (torso_w*(1 - (k - 4)/(segments - 3.5)), -k*pitch) minor b
          polyline closed bar = (-wb/2, -(k + 0.5)*pitch) (wb/2, -(k + 0.5)*pitch) (wb/2, -(k - 0.5)*pitch) (-wb/2, -(k - 0.5)*pitch)
          circle df = (0, -(k - 0.5)*pitch) radius rt
          circle dr = (0, -(k + 0.5)*pitch) radius rt
          fix e bar df dr
        }
        extrude TailWide{k} from TailWideSk{k} distance T new -> body Seg{k}
      }
      if torso_w*(1 - (k - 4)/(segments - 3.5)) < b + 0.2 mm {
        sketch TailSk{k} on XY {
          ellipse e = (0, -k*pitch) major (0, -k*pitch + b) minor (max(2.2 mm, torso_w*(1 - (k - 4)/(segments - 3.5))))
          polyline closed bar = (-wb/2, -(k + 0.5)*pitch) (wb/2, -(k + 0.5)*pitch) (wb/2, -(k - 0.5)*pitch) (-wb/2, -(k - 0.5)*pitch)
          circle df = (0, -(k - 0.5)*pitch) radius rt
          circle dr = (0, -(k + 0.5)*pitch) radius rt
          fix e bar df dr
        }
        extrude Tail{k} from TailSk{k} distance T new -> body Seg{k}
      }
      // The last segment ends at its own tip: trim the spine bar and disc drawn behind it
      if k == segments - 1 {
        box TailEnd{k} size (2*rt + 2 mm) pitch (T + 2 mm) corner (-rt - 1 mm, -k*pitch - b + 0.8 mm - pitch, -1 mm) cut targets Seg{k}
      }
    }

    // Legs on segments 2 (front) and 4 (hind): a thigh on the hip pivot (the torso is its A) and,
    // with leg_joints = 2, a foot with three toes on the knee pivot (the thigh is its A)
    if (k - 2)*(k - 4) == 0 {
      for s from 0 to 1 {
        if leg_joints == 1 {
          sketch LegSk{k}{s} on XY {
            slot t = centers ((1 - 2*s)*(torso_w + 2 mm), -k*pitch) ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) width lw
            circle h = ((1 - 2*s)*(torso_w + 2 mm), -k*pitch) radius rt
            circle kn = ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) radius rt
            slot f = centers ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) width lw
            slot ta = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 21 mm), -k*pitch + (3 - k)*10 mm) width 2.6 mm
            slot tb = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 23 mm), -k*pitch + (3 - k)*6 mm) width 2.6 mm
            slot tc = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 21.5 mm), -k*pitch + (3 - k)*2.5 mm) width 2.6 mm
            fix t h kn f ta tb tc
          }
          extrude Leg{k}{s} from LegSk{k}{s} distance T new -> body Thigh{k}{s}
        }
        if leg_joints == 2 {
          sketch ThighSk{k}{s} on XY {
            slot t = centers ((1 - 2*s)*(torso_w + 2 mm), -k*pitch) ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) width lw
            circle h = ((1 - 2*s)*(torso_w + 2 mm), -k*pitch) radius rt
            circle kn = ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) radius rt
            fix t h kn
          }
          extrude Thigh{k}{s} from ThighSk{k}{s} distance T new -> body Thigh{k}{s}
          sketch FootSk{k}{s} on XY {
            circle kn = ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) radius rt
            slot f = centers ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm) ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) width lw
            slot ta = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 21 mm), -k*pitch + (3 - k)*10 mm) width 2.6 mm
            slot tb = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 23 mm), -k*pitch + (3 - k)*6 mm) width 2.6 mm
            slot tc = centers ((1 - 2*s)*(torso_w + 17 mm), -k*pitch + (3 - k)*6 mm) ((1 - 2*s)*(torso_w + 21.5 mm), -k*pitch + (3 - k)*2.5 mm) width 2.6 mm
            fix kn f ta tb tc
          }
          extrude Foot{k}{s} from FootSk{k}{s} distance T new -> body Foot{k}{s}
          cylinder Knee{k}{s}CutA radius (rt + gap) height (T - t1 + 1 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, t1) axis Z cut targets Thigh{k}{s}
          cylinder Knee{k}{s}Stem radius rs height (T - hh - t1 + 0.4 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, t1 - 0.2 mm) axis Z join targets Thigh{k}{s}
          cone Knee{k}{s}Mushroom radius (rs - 0.2 mm) top_radius (rs + hh) height (hh + 0.2 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, T - hh - 0.2 mm) axis Z join targets Thigh{k}{s}
          cylinder Knee{k}{s}CutB radius (rt + gap) height (t1 + gap + 1 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, -1 mm) axis Z cut targets Foot{k}{s}
          cylinder Knee{k}{s}Hole radius (rs + gap) height (T - t1 + 1 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, t1 + gap - 0.1 mm) axis Z cut targets Foot{k}{s}
          cone Knee{k}{s}Sink radius (rs - 0.5 mm + gap*sqrt(2)) top_radius (rs + hh + 0.5 mm + gap*sqrt(2)) height (hh + 1 mm) base ((1 - 2*s)*(torso_w + 11 mm), -k*pitch + (3 - k)*5 mm, T - hh - 0.5 mm) axis Z cut targets Foot{k}{s}
        }
        // Hip pivot
        cylinder Hip{k}{s}CutA radius (rt + gap) height (T - t1 + 1 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, t1) axis Z cut targets Seg{k}
        cylinder Hip{k}{s}Stem radius rs height (T - hh - t1 + 0.4 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, t1 - 0.2 mm) axis Z join targets Seg{k}
        cone Hip{k}{s}Mushroom radius (rs - 0.2 mm) top_radius (rs + hh) height (hh + 0.2 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, T - hh - 0.2 mm) axis Z join targets Seg{k}
        cylinder Hip{k}{s}CutB radius (rt + gap) height (t1 + gap + 1 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, -1 mm) axis Z cut targets Thigh{k}{s}
        cylinder Hip{k}{s}Hole radius (rs + gap) height (T - t1 + 1 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, t1 + gap - 0.1 mm) axis Z cut targets Thigh{k}{s}
        cone Hip{k}{s}Sink radius (rs - 0.5 mm + gap*sqrt(2)) top_radius (rs + hh + 0.5 mm + gap*sqrt(2)) height (hh + 1 mm) base ((1 - 2*s)*(torso_w + 2 mm), -k*pitch, T - hh - 0.5 mm) axis Z cut targets Thigh{k}{s}
      }
    }

    // Spine pivots: behind this segment it is A, in front of it B
    if k < segments - 1 {
      cylinder Rear{k}CutA radius (rt + gap) height (T - t1 + 1 mm) base (0, -(k + 0.5)*pitch, t1) axis Z cut targets Seg{k}
      cylinder Rear{k}Stem radius rs height (T - hh - t1 + 0.4 mm) base (0, -(k + 0.5)*pitch, t1 - 0.2 mm) axis Z join targets Seg{k}
      cone Rear{k}Mushroom radius (rs - 0.2 mm) top_radius (rs + hh) height (hh + 0.2 mm) base (0, -(k + 0.5)*pitch, T - hh - 0.2 mm) axis Z join targets Seg{k}
    }
    if k > 0 {
      cylinder Front{k}CutB radius (rt + gap) height (t1 + gap + 1 mm) base (0, -(k - 0.5)*pitch, -1 mm) axis Z cut targets Seg{k}
      cylinder Front{k}Hole radius (rs + gap) height (T - t1 + 1 mm) base (0, -(k - 0.5)*pitch, t1 + gap - 0.1 mm) axis Z cut targets Seg{k}
      cone Front{k}Sink radius (rs - 0.5 mm + gap*sqrt(2)) top_radius (rs + hh + 0.5 mm + gap*sqrt(2)) height (hh + 1 mm) base (0, -(k - 0.5)*pitch, T - hh - 0.5 mm) axis Z cut targets Seg{k}
    }
  }
}

// ==== Fits: every pair that meets at a pivot, and the legs against their neighbours
if segments > 1 {
  check Seg0 clearance Seg1 >= (gap - 0.03 mm)
}
if segments > 2 {
  check Seg1 clearance Seg2 >= (gap - 0.03 mm)
}
if segments > 3 {
  check Seg2 clearance Seg3 >= (gap - 0.03 mm)
}
if segments > 4 {
  check Seg3 clearance Seg4 >= (gap - 0.03 mm)
}
if segments > 5 {
  check Seg4 clearance Seg5 >= (gap - 0.03 mm)
}
if segments > 6 {
  check Seg5 clearance Seg6 >= (gap - 0.03 mm)
}
if segments > 7 {
  check Seg6 clearance Seg7 >= (gap - 0.03 mm)
}
if segments > 8 {
  check Seg7 clearance Seg8 >= (gap - 0.03 mm)
}
if segments > 9 {
  check Seg8 clearance Seg9 >= (gap - 0.03 mm)
}
if segments > 10 {
  check Seg9 clearance Seg10 >= (gap - 0.03 mm)
}
if segments > 11 {
  check Seg10 clearance Seg11 >= (gap - 0.03 mm)
}
if segments > 12 {
  check Seg11 clearance Seg12 >= (gap - 0.03 mm)
}
if segments > 13 {
  check Seg12 clearance Seg13 >= (gap - 0.03 mm)
}
if segments > 14 {
  check Seg13 clearance Seg14 >= (gap - 0.03 mm)
}
if segments > 15 {
  check Seg14 clearance Seg15 >= (gap - 0.03 mm)
}
if segments > 5 {
  check Thigh20 clearance Seg2 >= (gap - 0.03 mm)
  check Thigh20 clearance Seg1 >= (gap - 0.03 mm)
  check Thigh20 clearance Seg3 >= (gap - 0.03 mm)
  check Thigh21 clearance Seg2 >= (gap - 0.03 mm)
  check Thigh21 clearance Seg1 >= (gap - 0.03 mm)
  check Thigh21 clearance Seg3 >= (gap - 0.03 mm)
  check Thigh40 clearance Seg4 >= (gap - 0.03 mm)
  check Thigh40 clearance Seg5 >= (gap - 0.03 mm)
  check Thigh40 clearance Seg3 >= (gap - 0.03 mm)
  check Thigh41 clearance Seg4 >= (gap - 0.03 mm)
  check Thigh41 clearance Seg5 >= (gap - 0.03 mm)
  check Thigh41 clearance Seg3 >= (gap - 0.03 mm)
}
if leg_joints == 2 {
  if segments > 5 {
    check Foot20 clearance Thigh20 >= (gap - 0.03 mm)
    check Foot20 clearance Seg2 >= (gap - 0.03 mm)
    check Foot21 clearance Thigh21 >= (gap - 0.03 mm)
    check Foot21 clearance Seg2 >= (gap - 0.03 mm)
    check Foot40 clearance Thigh40 >= (gap - 0.03 mm)
    check Foot40 clearance Seg4 >= (gap - 0.03 mm)
    check Foot41 clearance Thigh41 >= (gap - 0.03 mm)
    check Foot41 clearance Seg4 >= (gap - 0.03 mm)
  }
}

color Seg0, Seg1, Seg2, Seg3, Seg4 "#5f9e4a"
material Seg0, Seg1, Seg2, Seg3, Seg4 PLA
