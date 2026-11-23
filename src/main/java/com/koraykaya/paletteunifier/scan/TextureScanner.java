package com.koraykaya.paletteunifier.scan;

import com.koraykaya.paletteunifier.model.Candidate;
import com.koraykaya.paletteunifier.model.MaterialGroup;
import com.koraykaya.paletteunifier.model.TextureRole;
import com.koraykaya.paletteunifier.stats.ReloadStats;
import net.minecraft.client.resource.metadata.AnimationResourceMetadata;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Finds the textures of one namespace that belong to a material group. */
public final class TextureScanner {
	private final List<MaterialGroup> groupsLongestFirst;
	private final List<Pattern> globalExclusions;
	private final Map<String, TextureRole> roleOverrides;
	private final boolean includeItems;

	public TextureScanner(List<MaterialGroup> groups, List<Pattern> globalExclusions,
	                      Map<String, TextureRole> roleOverrides, boolean includeItems) {
		this.groupsLongestFirst = new ArrayList<>(groups);
		// dark_oak must be tried before oak.
		this.groupsLongestFirst.sort((a, b) -> Integer.compare(b.token.length(), a.token.length()));
		this.globalExclusions = globalExclusions;
		this.roleOverrides = roleOverrides;
		this.includeItems = includeItems;
	}

	public List<Candidate> scan(ResourceManager rm, String ns, List<Pattern> nsExclusions, ReloadStats.Namespace stats) {
		List<Candidate> out = new ArrayList<>();
		scanFolder(rm, ns, "textures/block", nsExclusions, stats, out);
		if (includeItems) scanFolder(rm, ns, "textures/item", nsExclusions, stats, out);
		return out;
	}

	private void scanFolder(ResourceManager rm, String ns, String folder, List<Pattern> nsExclusions,
	                        ReloadStats.Namespace stats, List<Candidate> out) {
		Map<Identifier, Resource> found = rm.findResources(folder,
				id -> id.getNamespace().equals(ns) && id.getPath().endsWith(".png"));
		for (Map.Entry<Identifier, Resource> e : found.entrySet()) {
			Identifier full = e.getKey();
			String p = full.getPath();
			if (!p.startsWith("textures/")) continue;
			String path = p.substring("textures/".length(), p.length() - ".png".length()); // block/bench/oak_bench
			stats.scanned.incrementAndGet();

			MaterialGroup group = null;
			for (MaterialGroup g : groupsLongestFirst) {
				if (g.matches(path)) {
					group = g;
					break;
				}
			}
			if (group == null) continue;

			if (isExcluded(path, globalExclusions) || isExcluded(path, nsExclusions) || group.excluded(path)) {
				stats.excluded.incrementAndGet();
				continue;
			}
			if (isAnimated(e.getValue())) {
				stats.animated.incrementAndGet();
				continue;
			}
			Identifier id = new Identifier(ns, path);
			TextureRole role = roleOverrides.get(id.toString());
			if (role == null) role = RoleClassifier.classify(path);
			out.add(new Candidate(id, group, role));
		}
	}

	private static boolean isExcluded(String path, List<Pattern> patterns) {
		if (patterns == null) return false;
		for (Pattern p : patterns) {
			if (p.matcher(path).find()) return true;
		}
		return false;
	}

	private static boolean isAnimated(Resource resource) {
		try {
			return resource.getMetadata().decode(AnimationResourceMetadata.READER).isPresent();
		} catch (Exception ignored) {
			return false;
		}
	}
}
