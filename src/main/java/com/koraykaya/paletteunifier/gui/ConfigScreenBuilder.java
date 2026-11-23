package com.koraykaya.paletteunifier.gui;

import com.koraykaya.paletteunifier.PaletteUnifierClient;
import com.koraykaya.paletteunifier.config.ConfigLoader;
import com.koraykaya.paletteunifier.config.NamespaceConfig;
import com.koraykaya.paletteunifier.config.PaletteUnifierConfig;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Cloth Config screen. Saving writes the file and, unless disabled, reloads resources so changes show at once. */
public final class ConfigScreenBuilder {
	private ConfigScreenBuilder() {}

	/** Per-namespace mode including "inherit the global mode". */
	public enum NsMode { INHERIT, CONDITIONAL, FULL, OFF }

	public enum GlobalMode { CONDITIONAL, FULL, OFF }

	/** Mutable state edited by the screen and written back on save. */
	private static final class State {
		boolean applyOnSave = true;
	}

	public static Screen build(Screen parent) {
		PaletteUnifierConfig cfg = ConfigLoader.loadOrCreate();
		State state = new State();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Text.literal("Palette Unifier"))
				.setSavingRunnable(() -> {
					ConfigLoader.save(cfg);
					PaletteUnifierClient.CONFIG = cfg;
					if (state.applyOnSave) {
						MinecraftClient.getInstance().reloadResources();
					}
				});
		ConfigEntryBuilder eb = builder.entryBuilder();

		// ---------------------------------------------------------------- General
		ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
		general.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.enabled)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Master switch. Off = every texture is left exactly as the mods and resource packs ship it."))
				.setSaveConsumer(v -> cfg.enabled = v)
				.build());
		general.addEntry(eb.startEnumSelector(Text.literal("Mode"), GlobalMode.class, parseGlobal(cfg.mode))
				.setDefaultValue(GlobalMode.CONDITIONAL)
				.setEnumNameProvider(e -> Text.literal(pretty(e.name())))
				.setTooltip(Text.literal("Conditional: only pixels close to the mod's own wood colours change (safe for furniture).\nFull: whole-texture palette swap (only for pure-wood mods).\nOff: no recoloring."))
				.setSaveConsumer(v -> cfg.mode = v.name().toLowerCase(Locale.ROOT))
				.build());
		general.addEntry(eb.startDoubleField(Text.literal("Tolerance"), cfg.tolerance)
				.setDefaultValue(0.15)
				.setMin(0.0).setMax(1.0)
				.setTooltip(Text.literal("Colour distance (0..1) to the mod's wood palette for conditional mode.\n0.08 strict: dark outlines and shading may keep the old tint.\n0.25 loose: similar non-wood colours may get tinted."))
				.setSaveConsumer(v -> cfg.tolerance = v)
				.build());
		general.addEntry(eb.startDoubleField(Text.literal("Hue tolerance (degrees)"), cfg.hueTolerance)
				.setDefaultValue(18.0)
				.setMin(0.0).setMax(180.0)
				.setTooltip(Text.literal("How far a pixel's hue may be from the mod's wood hue and still count as wood.\nKeeps moss and coloured accents untouched. Raise if real wood is skipped."))
				.setSaveConsumer(v -> cfg.hueTolerance = v)
				.build());
		general.addEntry(eb.startDoubleField(Text.literal("Chroma guard"), cfg.chromaGuard)
				.setDefaultValue(0.55)
				.setMin(0.0).setMax(1.0)
				.setTooltip(Text.literal("Pixels less colourful than this fraction of the wood palette are treated as metal/grey and left alone.\nKeeps nails, iron bands and highlights. 0 disables."))
				.setSaveConsumer(v -> cfg.chromaGuard = v)
				.build());
		general.addEntry(eb.startBooleanToggle(Text.literal("Include item textures"), cfg.includeItems)
				.setDefaultValue(false)
				.setTooltip(Text.literal("Also recolor textures/item. Off by default; most item icons derive from block textures anyway."))
				.setSaveConsumer(v -> cfg.includeItems = v)
				.build());
		general.addEntry(eb.startBooleanToggle(Text.literal("Apply on save (reload resources)"), state.applyOnSave)
				.setDefaultValue(true)
				.setTooltip(Text.literal("Press F3+T later if you turn this off."))
				.setSaveConsumer(v -> state.applyOnSave = v)
				.build());

		// ---------------------------------------------------------------- Mods
		ConfigCategory mods = builder.getOrCreateCategory(Text.literal("Mods"));
		Set<String> all = new LinkedHashSet<>();
		if (cfg.knownNamespaces != null) all.addAll(cfg.knownNamespaces);
		if (cfg.namespaces != null) all.addAll(cfg.namespaces.keySet());
		List<String> sorted = new ArrayList<>(all);
		sorted.sort(String::compareTo);
		for (String ns : sorted) {
			NamespaceConfig nc = cfg.namespaces.get(ns);
			boolean enabled = nc == null || nc.enabled;
			NsMode mode = nc == null || nc.mode == null ? NsMode.INHERIT : parseNs(nc.mode);
			List<AbstractConfigListEntry> sub = new ArrayList<>();
			sub.add(eb.startBooleanToggle(Text.literal("Enabled"), enabled)
					.setDefaultValue(true)
					.setSaveConsumer(v -> namespace(cfg, ns).enabled = v)
					.build());
			sub.add(eb.startEnumSelector(Text.literal("Mode"), NsMode.class, mode)
					.setDefaultValue(NsMode.INHERIT)
					.setEnumNameProvider(e -> Text.literal(pretty(e.name())))
					.setSaveConsumer(v -> namespace(cfg, ns).mode = v == NsMode.INHERIT ? null : v.name().toLowerCase(Locale.ROOT))
					.build());
			mods.addEntry(eb.startSubCategory(Text.literal(ns), sub)
					.setExpanded(false)
					.build());
		}
		mods.addEntry(eb.startStrList(Text.literal("Scanned mod ids"), new ArrayList<>(cfg.knownNamespaces))
				.setDefaultValue(PaletteUnifierConfig.defaults().knownNamespaces)
				.setTooltip(Text.literal("Namespaces (mod ids) whose block textures are scanned. Add a mod id here to include it."))
				.setSaveConsumer(v -> cfg.knownNamespaces = new ArrayList<>(v))
				.build());

		// ---------------------------------------------------------------- Filters
		ConfigCategory filters = builder.getOrCreateCategory(Text.literal("Filters"));
		filters.addEntry(eb.startStrList(Text.literal("Exclusions (regex on texture path)"), new ArrayList<>(cfg.exclusions))
				.setDefaultValue(PaletteUnifierConfig.defaults().exclusions)
				.setTooltip(Text.literal("Textures whose path matches any of these are never recolored, e.g. leaves, sapling, _pie."))
				.setSaveConsumer(v -> cfg.exclusions = new ArrayList<>(v))
				.build());

		// ---------------------------------------------------------------- Advanced
		ConfigCategory adv = builder.getOrCreateCategory(Text.literal("Advanced"));
		adv.addEntry(eb.startBooleanToggle(Text.literal("Debug export"), cfg.debugExport)
				.setDefaultValue(false)
				.setTooltip(Text.literal("Write before/after PNGs to config/paletteunifier/debug/ on the next reload."))
				.setSaveConsumer(v -> cfg.debugExport = v)
				.build());
		adv.addEntry(eb.startIntField(Text.literal("Debug export limit"), cfg.debugExportMax)
				.setDefaultValue(200).setMin(0).setMax(100000)
				.setSaveConsumer(v -> cfg.debugExportMax = v)
				.build());
		adv.addEntry(eb.startIntField(Text.literal("Cache size (textures)"), cfg.cacheMaxTextures)
				.setDefaultValue(4000).setMin(0).setMax(100000)
				.setTooltip(Text.literal("Generated textures kept in memory so F3+T can skip unchanged work."))
				.setSaveConsumer(v -> cfg.cacheMaxTextures = v)
				.build());
		adv.addEntry(eb.startBooleanToggle(Text.literal("Log per mod"), cfg.logPerNamespace)
				.setDefaultValue(true)
				.setSaveConsumer(v -> cfg.logPerNamespace = v)
				.build());

		return builder.build();
	}

	private static NamespaceConfig namespace(PaletteUnifierConfig cfg, String ns) {
		return cfg.namespaces.computeIfAbsent(ns, k -> new NamespaceConfig());
	}

	private static GlobalMode parseGlobal(String s) {
		try {
			return GlobalMode.valueOf(s.trim().toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return GlobalMode.CONDITIONAL;
		}
	}

	private static NsMode parseNs(String s) {
		try {
			return NsMode.valueOf(s.trim().toUpperCase(Locale.ROOT));
		} catch (Exception e) {
			return NsMode.INHERIT;
		}
	}

	private static String pretty(String enumName) {
		String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
}
