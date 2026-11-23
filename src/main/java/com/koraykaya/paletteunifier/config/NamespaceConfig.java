package com.koraykaya.paletteunifier.config;

import java.util.ArrayList;
import java.util.List;

/** Per-mod overrides. */
public class NamespaceConfig {
	public boolean enabled = true;
	/** "conditional", "full" or "off"; null inherits. */
	public String mode;
	/** Lab distance tolerance; null inherits. */
	public Double tolerance;
	/** Hue tolerance in degrees; null inherits. */
	public Double hueTolerance;
	/** Chroma guard factor 0..1; null inherits. */
	public Double chromaGuard;
	/** Extra exclusion regexes applied to this namespace's texture paths. */
	public List<String> exclusions = new ArrayList<>();
	/**
	 * Explicit textures (ids without textures/ and .png) to build this mod's wood palette from, per group,
	 * e.g. "oak": ["handcrafted:block/bench/oak_bench"]. Overrides the automatic planks-like heuristic.
	 */
	public java.util.Map<String, List<String>> sourceTextures = new java.util.LinkedHashMap<>();

	public NamespaceConfig() {}

	public NamespaceConfig(boolean enabled, String mode) {
		this.enabled = enabled;
		this.mode = mode;
	}
}
