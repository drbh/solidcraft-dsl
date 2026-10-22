design RepeatedBrackets
param spacing = 30 mm
component Block {
  box Solid size 8 8 8 -> body BlockBody
}
component Pair {
  instance left = Block
  instance right = Block at (spacing,0,0)
}
instance first = Pair at (10,20,0) rotate Z 90 deg
instance second = Pair at (80,20,0)
ground first
ground second
