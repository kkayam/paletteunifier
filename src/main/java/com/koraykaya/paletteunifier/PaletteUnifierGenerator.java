package com.koraykaya.paletteunifier;

import com.koraykaya.paletteunifier.config.ConfigLoader;
import com.koraykaya.paletteunifier.config.MaterialGroupConfig;
import com.koraykaya.paletteunifier.config.NamespaceConfig;
import com.koraykaya.paletteunifier.config.PaletteUnifierConfig;
import com.koraykaya.paletteunifier.io.DebugExporter;
import com.koraykaya.paletteunifier.model.Candidate;
import com.koraykaya.paletteunifier.model.MaterialGroup;
import com.koraykaya.paletteunifier.model.RecolorMode;
import com.koraykaya.paletteunifier.model.TextureRole;
import com.koraykaya.paletteunifier.palette.PaletteCache;
import com.koraykaya.paletteunifier.palette.ReferencePalettes;
import com.koraykaya.paletteunifier.palette.SortedPalette;
import com.koraykaya.paletteunifier.palette.SourcePaletteBuilder;
import com.koraykaya.paletteunifier.palette.WoodPalette;
import com.koraykaya.paletteunifier.recolor.ConditionalRecolorer;
import com.koraykaya.paletteunifier.recolor.FullRecolorer;
import com.koraykaya.paletteunifier.scan.TextureScanner;
import com.koraykaya.paletteunifier.stats.ReloadStats;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynClientResourcesGenerator;
import net.mehvahdjukaar.moonlight.api.resources.pack.DynamicTexturePack;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceGenTask;
import net.mehvahdjukaar.moonlight.api.resources.pack.ResourceSink;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Moonlight dynamic-pack generator. Runs before atlas stitching on every resource reload, reads mod
 * textures and the reference wood through the live pack stack, and writes recolored copies to the
 * same ids in a pack that sits above everything else.
 */
public final class PaletteUnifierGenerator extends DynClientResourcesGenerator {
	private static final Logger LOGGER = LogManager.getLogger("PaletteUnifier");
	public static PaletteUnifierGenerator INSTANCE;

	private final PaletteCache cache = new PaletteCache(4000);
	private volatile ReloadStats lastStats = new ReloadStats();

	public PaletteUnifierGenerator() {
		super(new DynamicTexturePack(new Identifier(PaletteUnifierClient.MOD_ID, "generated_textures")));
		INSTANCE = this;
		declareNamespaces(PaletteUnifierClient.CONFIG);
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	public boolean runsOnEveryReload() {
		return true;
	}

	@Override
	public boolean shouldClearOnReload() {
		return true;
	}

	public ReloadStats lastStats() {
		return lastStats;
	}

	public PaletteCache cache() {
		return cache;
	}

	private Set<String> enabledNamespaces(PaletteUnifierConfig cfg) {
		Set<String> set = new LinkedHashSet<>();
		if (cfg.knownNamespaces != null) set.addAll(cfg.knownNamespaces);
		if (cfg.namespaces != null) set.addAll(cfg.namespaces.keySet());
		set.removeIf(ns -> {
			NamespaceConfig nc = cfg.namespaces == null ? null : cfg.namespaces.get(ns);
			return nc != null && !nc.enabled;
		});
		return set;
	}

	private void declareNamespaces(PaletteUnifierConfig cfg) {
		Set<String> set = enabledNamespaces(cfg);
		dynamicPack.addNamespaces(set.toArray(new String[0]));
	}

	@Override
	public void regenerateDynamicAssets(Consumer<ResourceGenTask> executor) {
		PaletteUnifierConfig cfg = ConfigLoader.loadOrCreate();
		PaletteUnifierClient.CONFIG = cfg;
		ReloadStats stats = new ReloadStats();
		lastStats = stats;
		if (!cfg.enabled) {
			LOGGER.info("Palette Unifier is disabled in config");
			return;
		}
		DebugExporter.resetCounter();
		cache.setMax(Math.max(0, cfg.cacheMaxTextures));

		List<MaterialGroup> groups = new ArrayList<>();
		for (MaterialGroupConfig g : cfg.groups) {
			if (g != null && g.name != null) groups.add(new MaterialGroup(g));
		}
		List<Pattern> globalEx = compile(cfg.exclusions);
		Map<String, TextureRole> overrides = new HashMap<>();
		for (Map.Entry<String, String> e : cfg.roleOverrides.entrySet()) {
			TextureRole r = TextureRole.fromKey(e.getValue());
			if (r != null) overrides.put(e.getKey(), r);
		}
		TextureScanner scanner = new TextureScanner(groups, globalEx, overrides, cfg.includeItems);
		RecolorMode globalMode = RecolorMode.parse(cfg.mode, RecolorMode.CONDITIONAL);

		Set<String> namespaces = enabledNamespaces(cfg);
		declareNamespaces(cfg);

		AtomicReference<ReferencePalettes> refs = new AtomicReference<>();
		AtomicInteger remaining = new AtomicInteger(namespaces.size());
		long start = System.nanoTime();

		for (String ns : namespaces) {
			NamespaceConfig nsCfg = cfg.namespaces == null ? null : cfg.namespaces.get(ns);
			executor.accept((rm, sink) -> {
				ReloadStats.Namespace st = stats.of(ns);
				long t0 = System.nanoTime();
				try {
					ReferencePalettes reference = refs.updateAndGet(r -> r != null ? r : new ReferencePalettes(rm));
					processNamespace(rm, sink, ns, nsCfg, cfg, globalMode, scanner, reference, st);
				} catch (Throwable t) {
					st.error(t, "namespace");
					LOGGER.error("Palette Unifier failed for namespace {}", ns, t);
				} finally {
					st.millis = (System.nanoTime() - t0) / 1_000_000L;
					if (cfg.logPerNamespace && (st.matched.get() > 0 || st.errors.get() > 0)) LOGGER.info(st.line());
					if (remaining.decrementAndGet() == 0) {
						stats.totalMillis = (System.nanoTime() - start) / 1_000_000L;
						LOGGER.info("Palette Unifier: recolored={} cached={} matched={} errors={} in {} ms",
								stats.total(n -> n.recolored.get()), stats.total(n -> n.cached.get()),
								stats.total(n -> n.matched.get()), stats.total(n -> n.errors.get()), stats.totalMillis);
					}
				}
			});
		}
	}

	private void processNamespace(ResourceManager rm, ResourceSink sink, String ns, NamespaceConfig nsCfg,
	                              PaletteUnifierConfig cfg, RecolorMode globalMode, TextureScanner scanner,
	                              ReferencePalettes reference, ReloadStats.Namespace st) {
		List<Pattern> nsEx = nsCfg == null ? List.of() : compile(nsCfg.exclusions);
		List<Candidate> candidates = scanner.scan(rm, ns, nsEx, st);
		st.matched.set(candidates.size());
		if (candidates.isEmpty()) return;

		RecolorMode nsMode = nsCfg == null ? null : RecolorMode.parse(nsCfg.mode, null);
		Map<String, WoodPalette> sourcePalettes = new HashMap<>();   // key group|role
		Set<String> noSource = new LinkedHashSet<>();

		for (Candidate c : candidates) {
			try {
				MaterialGroup g = c.group();
				RecolorMode mode = nsMode != null ? nsMode : (g.mode != null ? g.mode : globalMode);
				if (mode == RecolorMode.OFF) continue;
				double tol = nsCfg != null && nsCfg.tolerance != null ? nsCfg.tolerance
						: (g.tolerance != null ? g.tolerance : cfg.tolerance);
				double hueTol = nsCfg != null && nsCfg.hueTolerance != null ? nsCfg.hueTolerance
						: (g.hueTolerance != null ? g.hueTolerance : cfg.hueTolerance);
				double chromaGuard = nsCfg != null && nsCfg.chromaGuard != null ? nsCfg.chromaGuard
						: (g.chromaGuard != null ? g.chromaGuard : cfg.chromaGuard);

				Palette target = reference.get(g, c.role());
				if (target == null) {
					st.skippedNoPalette.incrementAndGet();
					continue;
				}
				WoodPalette source = null;
				if (mode == RecolorMode.CONDITIONAL) {
					String key = g.name + "|" + c.role().key;
					source = sourcePalettes.get(key);
					if (source == null && !noSource.contains(key)) {
						source = SourcePaletteBuilder.build(rm, ns, g, c.role(), candidates, nsCfg, hueTol, chromaGuard);
						if (source == null && c.role() != TextureRole.PLANKS) {
							source = SourcePaletteBuilder.build(rm, ns, g, TextureRole.PLANKS, candidates, nsCfg, hueTol, chromaGuard);
						}
						if (source == null) {
							// Nothing in this mod looks like plain wood: use the reference itself, which only
							// remaps pixels that are already close to the target.
							source = WoodPalette.of(target);
							noSource.add(key);
						}
						sourcePalettes.put(key, source);
					}
					if (source == null) source = WoodPalette.of(target);
				}

				long targetHash = new SortedPalette(target).hash;
				try (TextureImage input = TextureImage.open(rm, c.id())) {
					if (input.frameCount() > 1) {
						st.animated.incrementAndGet();
						continue;
					}
					long inputHash = PaletteCache.hash(input);
					PaletteCache.Entry hit = cache.get(c.id(), inputHash, targetHash);
					if (hit != null) {
						sink.addAndCloseTexture(c.id(), PaletteCache.toImage(hit));
						st.cached.incrementAndGet();
						continue;
					}
					TextureImage out = mode == RecolorMode.FULL
							? FullRecolorer.recolor(input, target)
							: ConditionalRecolorer.recolor(input, source, target, tol, hueTol, chromaGuard);
					if (cfg.debugExport) DebugExporter.export(c.id(), input, out, cfg.debugExportMax);
					cache.put(c.id(), inputHash, targetHash, out);
					sink.addAndCloseTexture(c.id(), out);
					st.recolored.incrementAndGet();
				}
			} catch (Throwable t) {
				st.error(t, c.id().toString());
			}
		}
		if (!st.errorSamples.isEmpty()) {
			LOGGER.warn("Palette Unifier: {} errors in {}; e.g. {}", st.errors.get(), ns, st.errorSamples);
		}
	}

	private static List<Pattern> compile(List<String> regexes) {
		List<Pattern> out = new ArrayList<>();
		if (regexes == null) return out;
		for (String s : regexes) {
			if (s == null || s.isBlank()) continue;
			try {
				out.add(Pattern.compile(s));
			} catch (Exception e) {
				LOGGER.warn("Bad exclusion regex '{}': {}", s, e.getMessage());
			}
		}
		return out;
	}
}
