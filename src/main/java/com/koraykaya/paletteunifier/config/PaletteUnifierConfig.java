package com.koraykaya.paletteunifier.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Root of config/paletteunifier.json5. Field names are the JSON keys. */
public class PaletteUnifierConfig {
	/** Bumped when defaults change in a way that needs migration. 0 here means "file predates versioning". */
	public int configVersion = 0;
	public static final int CURRENT_CONFIG_VERSION = 2;
	public boolean enabled = true;
	/** Also scan textures/item. Off by default: item icons rarely need it and some are foods. */
	public boolean includeItems = false;
	/** Global recolor mode: conditional | full | off. */
	public String mode = "conditional";
	/**
	 * Max Lab colour distance (Moonlight's 0..1 scale) between a pixel and the mod's wood palette for the
	 * pixel to be recolored in conditional mode. Needs tuning: lower leaves specks of the old colour,
	 * higher starts eating handles, glass and cushions.
	 */
	public double tolerance = 0.15;
	/**
	 * Max hue difference (degrees) between a pixel and the mod's wood palette for the pixel to count as
	 * wood. Keeps moss (60°+ away) and other coloured accents untouched. Raise if real wood is skipped.
	 */
	public double hueTolerance = 18.0;
	/**
	 * A pixel must have at least this fraction of the chroma (colourfulness) of the wood colour it is
	 * closest to, to count as wood. Keeps grey metal, nails and white highlights untouched. 0 disables the guard.
	 */
	public double chromaGuard = 0.55;
	/** Write before/after PNGs to config/paletteunifier/debug/<ns>/... for inspection. */
	public boolean debugExport = false;
	public int debugExportMax = 200;
	/** In-memory cache of generated textures, so F3+T mostly re-emits instead of recomputing. */
	public int cacheMaxTextures = 4000;
	/** Log one line per namespace with counts and timing. */
	public boolean logPerNamespace = true;

	/**
	 * Namespaces to scan. Scanning is per namespace so the list must be known up front.
	 * Namespaces listed under "namespaces" are scanned too.
	 */
	public List<String> knownNamespaces = new ArrayList<>(Arrays.asList(
			"chipped", "handcrafted", "railways", "vinery", "decorative_blocks", "regions_unexplored",
			"twigs", "farmersdelight", "friendsandfoes", "betternether", "supplementaries", "create",
			"betterend", "graveyard", "villagersplus", "derelict", "convenientdecor", "mcwholidays",
			"amendments", "enchanted", "brewery", "herbalbrews", "minecells", "promenade", "croptopia",
			"expandeddelight", "ends_delight", "chefsdelight", "things", "oxidized", "handcrafted",
			"macawsfurniture", "mcwdoors", "mcwtrpdoors", "mcwfences", "mcwwindows", "mcwroofs",
			"mcwbridges", "mcwlights", "mcwpaths", "comforts", "travelersbackpack", "lootr", "ironchests"
	));

	/** Global exclusion regexes, matched against the texture path (e.g. block/bench/oak_bench). */
	public List<String> exclusions = new ArrayList<>(Arrays.asList(
			"leaves", "leaf", "sapling", "propagule", "fungus", "roots", "vines?(_|$)", "_pie", "_juice", "_jam",
			"ingot", "nugget", "obsidian", "froglass", "nylium", "wart", "shroomlight", "weeping", "twisting",
			"cherry_blossom", "cherries", "cherry_(pie|juice|jam|bag|wine|grape)", "petals", "flower",
			"/track/", "portal_track", "_overlay", "_glow", "_emissive", "_e$"
	));

	/** "ns:block/path" -> "planks|log_side|log_top|stripped_side|stripped_top". */
	public Map<String, String> roleOverrides = new LinkedHashMap<>();

	public List<MaterialGroupConfig> groups = new ArrayList<>();

	public Map<String, NamespaceConfig> namespaces = new LinkedHashMap<>();

	public static PaletteUnifierConfig defaults() {
		PaletteUnifierConfig c = new PaletteUnifierConfig();
		c.configVersion = CURRENT_CONFIG_VERSION;
		for (String w : new String[]{"dark_oak", "oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry"}) {
			c.groups.add(MaterialGroupConfig.wood(w));
		}
		c.groups.add(MaterialGroupConfig.bamboo());
		c.groups.add(MaterialGroupConfig.netherWood("crimson"));
		c.groups.add(MaterialGroupConfig.netherWood("warped"));
		// Chipped has mossy, nailed and bound variants, so it uses the conditional mode like everyone else.
		c.namespaces.put("chipped", new NamespaceConfig(true, null));
		// Never touch vanilla itself (it is the reference).
		c.namespaces.put("minecraft", new NamespaceConfig(false, null));
		return c;
	}
}
