package com.koraykaya.paletteunifier.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.koraykaya.paletteunifier.PaletteUnifierClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads/writes config/paletteunifier.json5. Comments (// and /* *\/) are stripped before parsing. */
public final class ConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	public static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("paletteunifier.json5");
	public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("paletteunifier");

	private static final String HEADER = """
			// Palette Unifier
			// Recolors mod wood textures to match the wood of your active resource pack.
			// Edit and press F3+T (or /paletteunifier reload). Delete this file to regenerate defaults.
			//
			// mode:       conditional = only pixels close to the mod's own wood palette change (safe for furniture)
			//             full        = whole texture palette swap (only for pure-wood textures, e.g. chipped)
			//             off
			// tolerance:  Lab colour distance (0..1) for conditional mode. 0.05 = strict, 0.15 = loose.
			// groups:     material groups; refs are reference textures read from the ACTIVE pack stack.
			// namespaces: per-mod enabled/mode/tolerance/exclusions/sourceTextures overrides.
			// roleOverrides: "ns:block/path": planks | log_side | log_top | stripped_side | stripped_top
			""";

	private ConfigLoader() {}

	public static PaletteUnifierConfig loadOrCreate() {
		if (!Files.exists(FILE)) {
			PaletteUnifierConfig def = PaletteUnifierConfig.defaults();
			save(def);
			return def;
		}
		try {
			String raw = Files.readString(FILE, StandardCharsets.UTF_8);
			JsonReader reader = new JsonReader(new StringReader(stripComments(raw)));
			reader.setLenient(true);
			PaletteUnifierConfig cfg = GSON.fromJson(reader, PaletteUnifierConfig.class);
			if (cfg == null) throw new IOException("empty config");
			if (cfg.groups == null || cfg.groups.isEmpty()) {
				cfg.groups = PaletteUnifierConfig.defaults().groups;
			}
			if (cfg.namespaces == null) cfg.namespaces = PaletteUnifierConfig.defaults().namespaces;
			if (cfg.knownNamespaces == null) cfg.knownNamespaces = PaletteUnifierConfig.defaults().knownNamespaces;
			if (cfg.exclusions == null) cfg.exclusions = PaletteUnifierConfig.defaults().exclusions;
			if (cfg.roleOverrides == null) cfg.roleOverrides = new java.util.LinkedHashMap<>();
			if (migrate(cfg)) save(cfg);
			return cfg;
		} catch (Exception e) {
			PaletteUnifierClient.LOGGER.error("Could not read {}; using defaults. {}", FILE, e.toString());
			return PaletteUnifierConfig.defaults();
		}
	}

	/** Upgrades older config files in place. Returns true if something changed. */
	private static boolean migrate(PaletteUnifierConfig cfg) {
		boolean changed = false;
		if (cfg.configVersion < 2) {
			// 0.1/0.2 shipped Chipped in "full" mode, which wiped moss, nails and metal bands.
			NamespaceConfig chipped = cfg.namespaces.get("chipped");
			if (chipped != null && "full".equalsIgnoreCase(chipped.mode)) {
				chipped.mode = null;
				PaletteUnifierClient.LOGGER.info("Config migrated: chipped now uses conditional mode (keeps moss/nails/metal)");
			}
			// 0.1/0.2 defaults for the distance gate were tuned before the hue/chroma guards existed.
			if (cfg.tolerance == 0.08) cfg.tolerance = 0.15;
			cfg.configVersion = PaletteUnifierConfig.CURRENT_CONFIG_VERSION;
			changed = true;
		}
		return changed;
	}

	public static void save(PaletteUnifierConfig cfg) {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, HEADER + GSON.toJson(cfg) + "\n", StandardCharsets.UTF_8);
		} catch (IOException e) {
			PaletteUnifierClient.LOGGER.error("Could not write {}", FILE, e);
		}
	}

	/** Removes // line comments and /* block comments *\/ outside of strings. */
	static String stripComments(String s) {
		StringBuilder out = new StringBuilder(s.length());
		boolean inString = false, escape = false;
		int i = 0, n = s.length();
		while (i < n) {
			char c = s.charAt(i);
			if (inString) {
				out.append(c);
				if (escape) escape = false;
				else if (c == '\\') escape = true;
				else if (c == '"') inString = false;
				i++;
				continue;
			}
			if (c == '"') {
				inString = true;
				out.append(c);
				i++;
			} else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
				while (i < n && s.charAt(i) != '\n') i++;
			} else if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < n && !(s.charAt(i) == '*' && s.charAt(i + 1) == '/')) i++;
				i += 2;
			} else {
				out.append(c);
				i++;
			}
		}
		return out.toString();
	}
}
