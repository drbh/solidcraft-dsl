" Vim syntax file
" Language:     SolidCraft design language (.sc)
" Maintainer:   SolidCraft contributors
" Filenames:    *.sc
" See:          tools/solidcraft-dsl/README.md ("The language")
"
" Ordering matters: when two items match at the same column the one defined LAST wins, so
" operators come before numbers (`-6`), the arrow (`->`) and comments (`//`).

if exists("b:current_syntax")
  finish
endif

syn case match
syn iskeyword @,48-57,_

" ---- words that appear everywhere --------------------------------------------------------

" option keys (from, distance, radius, at, edges, …)
syn keyword scKey       between and rotate value values limits opposed
syn keyword scStatement component instance connector ground joint
syn keyword scKey       from distance distance2 radius diameter at edges faces profiles targets body
syn keyword scKey       plane axis count count2 spacing spacing2 dir dir2 offset units on about
syn keyword scKey       taper angle clearance thread depth height width length major minor
syn keyword scKey       thickness fit sides size corner center base normal to position
syn keyword scKey       counterbore countersink features bodies tools points target type
syn keyword scKey       direction alignment alignment2 face start through end
syn match   scKey       "\<origin\ze\s*("
" modifiers (bare words that take no value)
syn keyword scModifier  closed infer construction rounded periodic inscribed circumscribed
syn keyword scModifier  centers ends symmetric negative positive through_all join cut intersect
syn keyword scModifier  new auto inside outside both drilled simple reversed reversed2 identical
syn keyword scModifier  copy keep_tools flip explicit chain circular
" planes, axes and the sketch origin
syn keyword scPlane     XY XZ YZ
syn keyword scAxis      X Y Z x y
syn keyword scOrigin    origin

" dotted entity references: outline.3.end, NAME.bottom_left, Sketch.line1
syn match   scReference "\<\h\w*\%(\.\%(\h\w*\|\d\+\)\)\+"

syn match   scOperator  "[-+*/=]"
syn match   scDelimiter "[(),]"

" numbers with an optional unit; a leading minus is part of the number only when nothing
" operand-like precedes it (as in the lexer)
syn match   scNumber    "[[:alnum:]_.)]\@<!-\?\%(\d\+\%(\.\d\+\)\?\|\.\d\+\)\%([eE][-+]\?\d\+\)\?\%(\s*\%(mm\|cm\|m\|in\|ft\|deg\|rad\)\>\)\?" contains=scUnit
syn match   scUnit      "\<\%(mm\|cm\|m\|in\|ft\|deg\|rad\)\>" contained
syn match   scUnits     "\%(\<units\s\+\)\@<=\%(mm\|cm\|m\|in\|ft\|deg\|rad\)\>"

syn match   scArrow     "->"
syn match   scBodyName  "\%(->\s*body\s\+\)\@<=\h\w*"

syn match   scString    '"[^"]*"'
syn match   scComment   "//.*$" contains=scTodo
syn keyword scTodo      TODO FIXME XXX NOTE contained

syn cluster scCommon contains=scKey,scModifier,scPlane,scAxis,scOrigin,scReference,scOperator,
      \ scDelimiter,scNumber,scUnits,scArrow,scBodyName,scString,scComment

" ---- top level ---------------------------------------------------------------------------

syn keyword scKeyword     design param let sketch
syn match   scDesignName  "\%(^\s*design\s\+\)\@<=\h\w*"
syn match   scParamName   "\%(^\s*\%(param\|let\)\s\+\)\@<=\h\w*"
syn match   scSketchName  '\%(^\s*sketch\s\+\)\@<=\%(\h\w*\|"[^"]*"\)'

syn match   scFeature     "^\s*\%(extrude\|revolve\|hole\|box\|cylinder\|sphere\|torus\|fillet\|chamfer\|shell\|mirror\|pattern\|combine\|move\|remove\|sweep\|loft\)\>"
syn match   scFeatureName '\%(^\s*\%(extrude\|revolve\|hole\|box\|cylinder\|sphere\|torus\|fillet\|chamfer\|shell\|mirror\|pattern\|combine\|move\|remove\|sweep\|loft\)\s\+\%(circular\s\+\)\?\)\@<=\%(\h\w*\|"[^"]*"\)'

" ---- inside a sketch block ---------------------------------------------------------------

" `[construction] line|polyline|rect|… [closed|infer|center|…] NAME =`
syn match   scSketchStmt  "^\s*\%(construction\s\+\)\?\%(line\|polyline\|rect\|circle\|arc\|point\|polygon\|slot\|spline\)\>\%(\s\+\%(closed\|infer\|center\|rounded\|periodic\|inscribed\|circumscribed\|centers\|ends\)\>\)*" contained contains=scSketchWord,scStmtModifier
syn keyword scSketchWord  line polyline rect circle arc point polygon slot spline contained
syn keyword scStmtModifier construction closed infer center rounded periodic inscribed circumscribed centers ends contained
syn match   scEntityName  "\%(^\s*\%(construction\s\+\)\?\%(line\|polyline\|rect\|circle\|arc\|point\|polygon\|slot\|spline\)\s\+\%(\%(closed\|infer\|center\|rounded\|periodic\|inscribed\|circumscribed\|centers\|ends\)\s\+\)*\)\@<=\h\w*\ze\s*=" contained
" `dim [auto|distance|horizontal|…] A [-> B] = expr`
syn match   scDim         "^\s*dim\>\%(\s\+\%(auto\|distance\|horizontal\|vertical\|length\|radius\|diameter\|angle\)\>\)\?" contained contains=scDimWord,scDimKind
syn keyword scDimWord     dim contained
syn keyword scDimKind     auto distance horizontal vertical length radius diameter angle contained
" constraints and the sketch fillet
syn match   scConstraint  "^\s*\%(horizontal\|vertical\|coincident\|tangent\|equal\|parallel\|perpendicular\|concentric\|collinear\|curvature\|midpoint\|symmetric\|fix\|fillet\)\>" contained

syn region  scSketchBlock matchgroup=scDelimiter start="{" end="}" fold
      \ contains=@scCommon,scSketchStmt,scEntityName,scDim,scConstraint

" ---- colours -----------------------------------------------------------------------------

hi def link scComment      Comment
hi def link scTodo         Todo
hi def link scString       String
hi def link scNumber       Number
hi def link scUnit         Special
hi def link scUnits        Special
hi def link scReference    Identifier
hi def link scKey          Label
hi def link scModifier     StorageClass
hi def link scStmtModifier StorageClass
hi def link scPlane        Constant
hi def link scAxis         Constant
hi def link scOrigin       Constant
hi def link scArrow        Operator
hi def link scOperator     Operator
hi def link scDelimiter    Delimiter
hi def link scKeyword      Statement
hi def link scDesignName   Type
hi def link scParamName    Define
hi def link scSketchName   Type
hi def link scBodyName     Type
hi def link scFeature      Structure
hi def link scFeatureName  Function
hi def link scSketchWord   Function
hi def link scEntityName   Define
hi def link scDimWord      Function
hi def link scDimKind      StorageClass
hi def link scConstraint   Function

let b:current_syntax = "solidcraft"

hi def link scStatement Statement
