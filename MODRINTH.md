# Palette Unifier

**Every mod's oak finally looks like the same oak.**

Furniture, cabinets, shelves, panels and trapdoors from different mods are all "made of oak", yet each
mod drew its own shade of it. Add a resource pack that restyles vanilla wood and the mismatch gets
worse. Palette Unifier recolors mod wood textures at load time so they match the wood of **whatever
resource pack you have active**.

## What it does
- Covers the 11 vanilla wood types: oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry,
  bamboo, crimson and warped.
- Reads the reference colours from your live resource pack stack, so switching packs re-matches
  everything on the next reload.
- Only recolors pixels that are actually wood. Iron handles, nails, gold bars, moss, glass and
  cushions keep their own colours.
- Client-side only. Textures are generated in memory on each resource reload; nothing in your world,
  your mods or your resource packs is modified. Remove the mod and everything is back to normal.
- No FPS cost while playing. The work happens during resource loading (a couple of seconds for a
  large pack, mostly cached afterwards).

## Requirements
- Fabric Loader and Fabric API
- [Moonlight Lib](https://modrinth.com/mod/moonlight)
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) and
  [Cloth Config](https://modrinth.com/mod/cloth-config) for the in-game settings screen

## Settings
Open Mods → Palette Unifier → Configure, or edit `config/paletteunifier.json5`.

- **Enabled**: master switch.
- **Mode**: conditional (default, keeps non-wood details), full (whole-texture swap) or off, globally
  and per mod.
- **Tolerance / Hue tolerance / Chroma guard**: how strictly a pixel must look like wood to be
  recolored.
- **Mods**: per-mod toggle, and the list of mod ids that are scanned. Add a mod id to include it.
- **Filters**: regex exclusions for things with a wood name that aren't wood (leaves, saplings, pies).

## Commands
- `/paletteunifier on` · `off` · `reload` · `stats`
- `/paletteunifier debug on|off` writes before/after PNGs to `config/paletteunifier/debug/`
- `/paletteunifier export <mod id>` dumps that mod's generated textures

## Compatibility
Works with Sodium, Iris and other resource-pack based mods. Animated textures and item icons are
skipped by default. If a block looks wrong, turn that mod off in the settings or add an exclusion,
and please report it with the debug export.
