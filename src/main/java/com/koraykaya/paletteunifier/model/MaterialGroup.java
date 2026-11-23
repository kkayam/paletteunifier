package com.koraykaya.paletteunifier.model;

import com.koraykaya.paletteunifier.config.MaterialGroupConfig;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Compiled form of a {@link MaterialGroupConfig}. */
public final class MaterialGroup {
	public final String name;
	public final String token;
	public final Pattern tokenPattern;
	public final EnumMap<TextureRole, Identifier> refs = new EnumMap<>(TextureRole.class);
	public final List<Pattern> exclusions = new ArrayList<>();
	public final RecolorMode mode;      // null = inherit
	public final Double tolerance;      // null = inherit
	public final Double hueTolerance;   // null = inherit
	public final Double chromaGuard;    // null = inherit

	public MaterialGroup(MaterialGroupConfig cfg) {
		this.name = cfg.name;
		this.token = cfg.token == null ? cfg.name : cfg.token;
		// Token must be delimited by start/end, underscore or slash so "cloak" never matches "oak".
		this.tokenPattern = Pattern.compile("(^|_|/)" + Pattern.quote(this.token) + "(_|$|/|\\.)");
		if (cfg.refs != null) {
			for (Map.Entry<String, String> e : cfg.refs.entrySet()) {
				TextureRole role = TextureRole.fromKey(e.getKey());
				if (role != null && e.getValue() != null && !e.getValue().isBlank()) {
					refs.put(role, new Identifier(e.getValue()));
				}
			}
		}
		if (cfg.exclusions != null) {
			for (String s : cfg.exclusions) {
				if (s != null && !s.isBlank()) exclusions.add(Pattern.compile(s));
			}
		}
		this.mode = cfg.mode == null ? null : RecolorMode.parse(cfg.mode, null);
		this.tolerance = cfg.tolerance;
		this.hueTolerance = cfg.hueTolerance;
		this.chromaGuard = cfg.chromaGuard;
	}

	public boolean matches(String path) {
		return tokenPattern.matcher(path).find();
	}

	public boolean excluded(String path) {
		for (Pattern p : exclusions) {
			if (p.matcher(path).find()) return true;
		}
		return false;
	}

	/** Reference texture for the role, walking the fallback chain. Null only if the group has no refs at all. */
	public Identifier referenceFor(TextureRole role) {
		TextureRole r = role;
		while (r != null) {
			Identifier id = refs.get(r);
			if (id != null) return id;
			r = r.fallback();
		}
		return refs.get(TextureRole.PLANKS);
	}
}
