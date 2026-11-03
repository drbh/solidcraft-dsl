// 34. Shop-vac hose adapter: a 32 mm hose cuff on one end, a 35 mm tool spigot on the other.
//
// Z is the flow axis. The part is one revolved profile (sketched on XZ, where sketch v = -z).
// From z = 0 up: a female socket whose bore tapers from sock_mouth to sock_deep, so a tool with a
// nominal 35 mm spigot wedges in partway and seals; a conical transition; then a male spigot whose
// outside tapers from male_base to male_tip, so it wedges into the hose cuff's 32 mm bore. The
// shoulder where the outer cone meets the spigot is the hose cuff's stop. Both tapers are about
// 0.75 deg per side, the usual friction-fit angle for vacuum fittings; change the four fit
// diameters to suit a cuff and tool measured with calipers.
//
// Print standing on the socket mouth (z = 0), no supports. Every outside surface narrows going up,
// and the inside steps from the socket bore to the spigot bore through a cone steeper than 45 deg,
// so there is no overhang anywhere. The lead-in chamfers at both mouths also hide elephant foot.
design HoseAdapter units mm
// Measure these: the tool spigot's OD and the hose cuff's ID
param tool_od = 35 mm
param cuff_id = 32 mm
// Fit: how much the tapers go past nominal at each end
param fit = 0.4 mm
param sock_len = 32 mm
param male_len = 30 mm
param trans_len = 12 mm
param wall = 2.4 mm
param tip_wall = 2 mm
param lead = 1 mm
// Socket bore, wide at the mouth, tight at the bottom
let sock_mouth = tool_od + fit
let sock_deep = tool_od - fit
// Spigot outside, fat at the shoulder, slim at the tip
let male_base = cuff_id + fit
let male_tip = cuff_id - fit
let r_out = sock_mouth/2 + wall
let r_bore = male_tip/2 - tip_wall
let z1 = sock_len
let z2 = sock_len + trans_len
let z3 = z2 + male_len

// Half section, outside then inside, revolved about Z; the transition cone runs from z1 to z2
sketch Section on XZ {
  polyline closed half = (sock_mouth/2 + lead, 0) (r_out, 0) (r_out, -z1) (male_base/2, -z2) (male_tip/2, -(z3 - lead)) (male_tip/2 - lead, -z3) (r_bore, -z3) (r_bore, -z2) (sock_deep/2, -z1) (sock_mouth/2, -lead)
  fix half.1 half.2 half.3 half.4 half.5 half.6 half.7 half.8 half.9 half.10
}
revolve Body from Section axis Z -> body Adapter
