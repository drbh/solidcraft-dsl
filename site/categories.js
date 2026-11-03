// The gallery's sections and their order. Each entry is an example's file name without `.sc`;
// within a section, examples run roughly from simplest to most involved. File numbers are kept as
// stable IDs, so the order here is free to change. Examples not listed here land in "Other", and
// names listed here that don't exist are skipped, so the build never fails on this file.
export default [
  {
    title: 'Basics',
    blurb: 'Sketches, extrudes, holes and patterns: the first things to read.',
    examples: [
      '01_spacer', 'plate', '02_centered_plate', '03_bolt_plate', 'bracket', '04_stepped_shaft',
      '05_trigonometric_gusset', '06_bearing_flange', '07_slotted_bracket', '30_open_box',
      '08_enclosure_and_lid', '09_heat_sink', '10_rotating_link', '12_spoked_pulley',
      '29_thin_sheet', '51_sheet_metal_bracket', '57_perforated_grille', '62_hat_channel_mount',
    ],
  },
  {
    title: 'Language and engine features',
    blurb: 'One feature at a time: sketch commands, construction geometry, surfaces, assemblies.',
    examples: [
      '70_sketch_commands', '72_more_sketch_commands', '74_sketch_curves', '75_sketch_editing',
      '69_construction_geometry', '76_construction_lines', '71_more_construction_and_surfaces',
      '73_surface_blends_and_sheet_queries', '66_engine_commands', '68_more_engine_commands',
      '67_channel_block_and_sleeve', 'assembly_hinge', 'assembly_nested',
    ],
  },
  {
    title: 'Print fit and tolerances',
    blurb: 'Test pieces and print-in-place joints that set the clearances everything else uses.',
    examples: [
      '46_tolerance_coupon', '60_hole_fit_ladder', '47_print_in_place_hinge_box',
      '48_living_hinge_pillbox', '120_printed_bearing', '143_thread_and_radius_gauges',
      '151_nut_and_bolt_set', '150_locking_ball_joint',
    ],
  },
  {
    title: 'Gears and mechanisms',
    blurb: 'Involute gears, gearboxes, linkages and intermittent motion.',
    examples: [
      '37_involute_gears', '13_planetary_gearbox', '14_printable_gearbox', '18_skeleton_gearbox',
      '19_mini_skeleton_gearbox', '19_mini_skeleton_gearbox_assembly', '15_cycloidal_drive',
      '141_differential_fidget', '17_servo_arm', '16_strandbeest', '118_geneva_clock',
      '117_kinetic_wave', '121_bistable_clicker',
    ],
  },
  {
    title: 'Workshop and tools',
    blurb: 'Clamps, holders, jigs and tools for the bench.',
    examples: [
      '34_hose_adapter', '20_table_pole_clamp', '43_spool_holder', '45_hex_bit_organizer',
      '31_bike_wheel_holder', '112_drag_chain', '140_bench_vise', '157_heat_set_insert_press',
      '123_camera_slider',
    ],
  },
  {
    title: 'Storage and organizers',
    blurb: 'Bins, drawers, wall systems and boxes, most of them stackable or modular.',
    examples: [
      '40_grid_bins', '54_nesting_tray', '44_stackable_drawers', '127_spice_drawer_insert',
      '147_battery_crate', '138_rugged_box', '153_under_desk_drawer', '41_pegboard_holder',
      '113_french_cleat_system', '105_hex_wall_shelves', '55_split_wall_shelf',
      '42_cable_tray_clips', '56_arc_cable_comb', '148_gravity_broom_holder',
      '24_uppdatera_riser', '25_uppdatera_riser_v2', '28_uppdatera_riser_v5',
    ],
  },
  {
    title: 'Home and kitchen',
    blurb: 'Everyday things: jars, lids, clips, planters, frames and desk pieces.',
    examples: [
      '58_logo_coaster', '50_name_tag', '53_desk_name_plate', '36_soap_dish', '35_ergonomic_knob',
      '65_domed_knob', '33_swept_wall_hook', '155_bag_clip', '61_kitchen_funnel', '21_bowl_lid',
      '49_bottle_and_cap', '85_mason_jar', '89_tupperware_lid_holder', '142_toothpaste_squeezer',
      '64_bent_tray', '63_thin_wall_vase', '32_lofted_planter', '116_self_watering_planter',
      '86_shoe_horn', '87_tweezers', '145_card_wallet', '88_picture_frame', '11_phone_stand',
      '125_headphone_stand', '82_desk_lamp', '126_birdhouse',
    ],
  },
  {
    title: 'Furniture',
    blurb: 'Full-size furniture, from a plywood stool to a jointed lounge chair.',
    examples: [
      '102_plywood_stool', '101_cantilever_chair', '104_rocking_chair', '100_tensegrity_table',
      '103_lamella_pendant', '78_spider_chair',
    ],
  },
  {
    title: 'Electronics enclosures',
    blurb: 'Cases, panels and grilles around real boards and modules.',
    examples: [
      '59_pcb_box', '52_vented_lid', '39_esp32_enclosure', '124_board_enclosure', '38_fan_guard',
      '114_eurorack_panel', '115_macropad', '22_sp404_mk3', '23_sp404_mini', '27_rc_car',
      '26_chappie_head',
    ],
  },
  {
    title: 'Retro gadgets',
    blurb: 'Buildable 2000s devices: real enclosures around displays, boards and batteries.',
    examples: [
      '99_usb_thumb_drive', '132_floppy_disk', '136_cassette_tape', '83_tamagotchi',
      '96_clip_player', '133_clip_cartridge_player', '80_ipod', '98_portable_cd_player',
      '135_rugged_watch', '90_flip_phone', '93_qwerty_phone', '131_swivel_phone',
      '94_motion_remote', '97_point_and_shoot', '95_pocket_camcorder', '79_gameboy',
      '91_dual_screen_handheld', '92_widescreen_handheld', '137_graphing_calculator',
      '81_macbook',
    ],
  },
  {
    title: 'Toys and fidgets',
    blurb: 'Print-in-place fidgets, articulated figures and classic toys.',
    examples: [
      '128_yoyo', '156_turbine_whistle', '146_captive_rings', '144_infinity_cube',
      '139_twist_egg_fidget', '152_flexi_lizard', '77_articulated_figure', '149_collapsing_sword',
      '154_pull_copter', '134_fingerboard', '119_puzzle_box', '129_kaleidoscope', '122_ocarina',
      '84_wagon',
    ],
  },
  {
    title: 'Building systems and tracks',
    blurb: 'Parts that compose: bricks, beams, hubs, track and marble runs.',
    examples: [
      '106_building_bricks', '110_beam_axle_set', '111_dowel_hubs', '107_train_track',
      '108_car_track', '109_marble_run', '130_marble_screw_lift',
    ],
  },
];
