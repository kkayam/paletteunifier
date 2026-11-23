package com.koraykaya.paletteunifier.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One material group as written in the config file. */
public class MaterialGroupConfig {
	/** Display/log name, e.g. "oak". */
	public String name;
	/** Name token to look for in texture paths. Defaults to name. */
	public String token;
	/** Role -> reference texture id (without textures/ and .png), e.g. "planks": "minecraft:block/oak_planks". */
	public Map<String, String> refs = new LinkedHashMap<>();
	/** Extra exclusion regexes for this group only. */
	public List<String> exclusions = new ArrayList<>();
	/** "conditional", "full" or "off"; null inherits the global mode. */
	public String mode;
	/** Lab distance tolerance override; null inherits the global tolerance. */
	public Double tolerance;
	/** Hue tolerance (degrees) override; null inherits. */
	public Double hueTolerance;
	/** Chroma guard factor override; null inherits. */
	public Double chromaGuard;

	public MaterialGroupConfig() {}

	/** Standard vanilla wood: oak, spruce, ... with log/stripped_log naming. */
	public static MaterialGroupConfig wood(String name) {
		MaterialGroupConfig g = new MaterialGroupConfig();
		g.name = name;
		g.token = name;
		g.refs.put("planks", "minecraft:block/" + name + "_planks");
		g.refs.put("log_side", "minecraft:block/" + name + "_log");
		g.refs.put("log_top", "minecraft:block/" + name + "_log_top");
		g.refs.put("stripped_side", "minecraft:block/stripped_" + name + "_log");
		g.refs.put("stripped_top", "minecraft:block/stripped_" + name + "_log_top");
		return g;
	}

	/** Nether wood: crimson/warped use stem/hyphae naming. */
	public static MaterialGroupConfig netherWood(String name) {
		MaterialGroupConfig g = new MaterialGroupConfig();
		g.name = name;
		g.token = name;
		g.refs.put("planks", "minecraft:block/" + name + "_planks");
		g.refs.put("log_side", "minecraft:block/" + name + "_stem");
		g.refs.put("log_top", "minecraft:block/" + name + "_stem_top");
		g.refs.put("stripped_side", "minecraft:block/stripped_" + name + "_stem");
		g.refs.put("stripped_top", "minecraft:block/stripped_" + name + "_stem_top");
		return g;
	}

	public static MaterialGroupConfig bamboo() {
		MaterialGroupConfig g = new MaterialGroupConfig();
		g.name = "bamboo";
		g.token = "bamboo";
		g.refs.put("planks", "minecraft:block/bamboo_planks");
		g.refs.put("log_side", "minecraft:block/bamboo_block");
		g.refs.put("log_top", "minecraft:block/bamboo_block_top");
		g.refs.put("stripped_side", "minecraft:block/stripped_bamboo_block");
		g.refs.put("stripped_top", "minecraft:block/stripped_bamboo_block_top");
		// Living bamboo plants, shoots and the mosaic are not planks.
		g.exclusions.add("bamboo_(stalk|stage|singleleaf|large_leaves|small_leaves|shoot|mosaic)");
		return g;
	}
}
