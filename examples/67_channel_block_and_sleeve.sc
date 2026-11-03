// 67. Cable-channel block and a print-in-place sleeve: direct edits of existing faces.
//
// The block's top face is split by two planes into three strips, and the middle strip is pulled
// down to make a channel for a cable: `split_face` cuts the face without changing the solid, and
// `press_pull` moves just that part of it (negative cuts, positive adds).
//
// The sleeve turns on a pin printed in place. Its bore is the pin's own side copied out by the
// clearance (`offset_surface`), thickened into the sleeve wall, so the gap is exactly `gap` all
// round whatever the pin's size. A cylinder's side is two half-faces: both are picked.
design ChannelBlockAndSleeve units mm
param L = 60 mm
param W = 40 mm
param H = 20 mm
param channel_w = 20 mm
param channel_d = 5 mm
param pin_r = 4 mm
param pin_h = 30 mm
param gap = 0.3 mm
param sleeve_t = 2 mm

box Blank size L W H corner (0,0,0) -> body Block
split_face SplitA faces (L/2, W/2, H) of Block origin ((L - channel_w)/2, 0, 0) normal (1, 0, 0)
split_face SplitB faces (L/2, W/2, H) of Block origin ((L + channel_w)/2, 0, 0) normal (1, 0, 0)
press_pull Channel face (L/2, W/2, H) distance (-channel_d)

cylinder Pin radius pin_r height pin_h base (L + 20 mm, W/2, 0) -> body Pin
offset_surface Bore faces (L + 20 mm + pin_r, W/2, pin_h/2) (L + 20 mm - pin_r, W/2, pin_h/2) of Pin distance gap -> body BoreSkin
thicken Wall bodies BoreSkin thickness sleeve_t new -> body Sleeve
remove DropBoreSkin bodies BoreSkin

check Block volume == (L*W*H - channel_w*W*channel_d)
check Sleeve clearance Pin >= gap
check Sleeve clearance Pin <= (gap + 0.001 mm)
// The sleeve's volume, to the engine's 0.01 % integration of curved faces
let sleeve_v = pi*((pin_r + gap + sleeve_t)^2 - (pin_r + gap)^2)*pin_h
check Sleeve volume >= (sleeve_v*0.9998)
check Sleeve volume <= (sleeve_v*1.0002)
