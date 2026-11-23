package com.koraykaya.paletteunifier.model;

import java.util.Locale;

public enum RecolorMode {
	/** Only pixels close to the mod's own wood palette are remapped. Safe for mixed-material textures. */
	CONDITIONAL,
	/** Whole-texture palette swap via Moonlight's Respriter. Only for textures that are pure wood. */
	FULL,
	/** Leave textures alone. */
	OFF;

	public static RecolorMode parse(String s, RecolorMode def) {
		if (s == null) return def;
		try {
			return valueOf(s.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return def;
		}
	}
}
