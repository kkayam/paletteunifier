package com.koraykaya.paletteunifier.model;

import java.util.Locale;

/** Which reference texture a wood texture should be matched against. */
public enum TextureRole {
	PLANKS("planks"),
	LOG_SIDE("log_side"),
	LOG_TOP("log_top"),
	STRIPPED_SIDE("stripped_side"),
	STRIPPED_TOP("stripped_top");

	public final String key;

	TextureRole(String key) {
		this.key = key;
	}

	public static TextureRole fromKey(String key) {
		if (key == null) return null;
		String k = key.trim().toLowerCase(Locale.ROOT);
		for (TextureRole r : values()) {
			if (r.key.equals(k) || r.name().equalsIgnoreCase(k)) return r;
		}
		return null;
	}

	/** Reasonable fallback when a group lacks a reference for this role. */
	public TextureRole fallback() {
		return switch (this) {
			case PLANKS -> null;
			case LOG_TOP -> LOG_SIDE;
			case STRIPPED_SIDE -> LOG_SIDE;
			case STRIPPED_TOP -> STRIPPED_SIDE;
			case LOG_SIDE -> PLANKS;
		};
	}
}
