# Palette Unifier

Client-side Fabric mod for Minecraft 1.20.1 that recolors every mod's wood textures (oak, spruce,
birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, warped) so they match the wood of
your **active resource pack**. Furniture, shelves, cabinets and panels from different mods finally look
like the same oak.

Built on Moonlight Lib's dynamic texture pack. Nothing touches your world: the recolored textures exist
only in memory, generated on every resource reload (F3+T), and removing the jar restores the originals.

## Requires
- Fabric Loader, Fabric API, Moonlight Lib (all already in Prominence II).

## In-game settings
With Mod Menu and Cloth Config installed (both are in Prominence II): Mods → Palette Unifier → Configure.
General has the on/off switch, mode and tolerance; Mods has a toggle and mode per mod; Filters holds the
exclusion regexes; Advanced has debug export and cache size. Saving reloads resources so you see the result at once.

## Config file: `config/paletteunifier.json5`
- `mode`: `conditional` (default, only pixels close to the mod's own wood palette change, so handles and
  glass stay), `full` (whole-texture palette swap, for pure-wood mods like Chipped) or `off`.
- `tolerance`: colour distance to the mod's wood palette for conditional mode (0.08 strict … 0.25 loose). Default 0.15.
- `hueTolerance`: degrees a pixel's hue may differ from the mod's wood hue (default 18). Keeps moss and
  coloured accents. Raise if real wood is being skipped.
- `chromaGuard`: pixels less colourful than this fraction of the nearest wood colour are treated as
  grey/metal and left alone (default 0.55). Pixels far more colourful than any wood tone (gold, paint)
  are left alone too. Keeps nails, iron bands, gold and highlights.
- `groups`: the material groups and their reference textures (read from the active pack).
- `namespaces`: per-mod `enabled`, `mode`, `tolerance`, `exclusions`, `sourceTextures`.
- `exclusions`: regexes for things that carry a wood name but aren't wood (leaves, saplings, pies…).
- `roleOverrides`: force a texture to match planks / log_side / log_top / stripped_side / stripped_top.
- `debugExport`: writes before/after PNGs to `config/paletteunifier/debug/`.

## Commands
- `/paletteunifier on` / `off` – master switch, applies immediately
- `/paletteunifier reload` – reload resources (same as F3+T)
- `/paletteunifier stats` – per-namespace counts and timing of the last reload
- `/paletteunifier debug on|off` – toggle debug export
- `/paletteunifier export <namespace>` – dump that mod's generated textures to `config/paletteunifier/export/`

## Build
`./gradlew build` → `build/libs/paletteunifier-<version>.jar`. `./gradlew runClient` starts a dev
client with Moonlight and Handcrafted for testing.
