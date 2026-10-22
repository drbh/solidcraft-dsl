design SharedHinge
param length = 30 mm
param opening = 35 deg
let half = length / 2

component Leaf {
  box Plate size length 12 3 corner (0,-6,0) -> body PlateBody
  connector pivot at (0,0,0) axis Z x X
  connector tip at (length,0,0) axis Z x X
}

instance fixed_leaf = Leaf at (10,20,0) rotate Z 15 deg
instance moving_leaf = Leaf at (10,20,3)
ground fixed_leaf
joint hinge revolute between fixed_leaf.pivot and moving_leaf.pivot value opening offset 3 mm limits (0 deg,160 deg)
