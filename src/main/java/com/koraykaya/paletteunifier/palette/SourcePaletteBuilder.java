package com.koraykaya.paletteunifier.palette;

import com.koraykaya.paletteunifier.PaletteUnifierClient;
import com.koraykaya.paletteunifier.config.NamespaceConfig;
import com.koraykaya.paletteunifier.model.Candidate;
import com.koraykaya.paletteunifier.model.MaterialGroup;
import com.koraykaya.paletteunifier.model.TextureRole;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds the palette that represents "this mod's idea of oak" (per namespace, group and role), so the
 * conditional recolorer knows which pixels are wood and which are handles, moss or nails.
 */
public final class SourcePaletteBuilder {
	/** Names that are almost certainly a flat wood surface, in order of preference. */
	private static final Pattern[] PREFERRED = {
			Pattern.compile("(^|/)[a-z_]*planks?(_|$|\\.|/)"),
			Pattern.compile("boards?(_|$|\\.|/)"),
			Pattern.compile("(table|desk|counter|shelf|bench)(_top)?(_|$|\\.|/)"),
			Pattern.compile("(cabinet|drawer|cupboard|wardrobe)_(top|side)"),
			Pattern.compile("(chair|stool|bench|nightstand)(_|$|\\.|/)"),
	};
	/** Above this many colours the picked texture is probably not pure wood; reduce to keep the gate tight. */
	private static final int MAX_SOURCE_COLORS = 12;

	private SourcePaletteBuilder() {}

	/**
	 * @param all all candidates of this namespace (any group/role)
	 * @return the purified source palette, or null when nothing usable exists
	 */
	public static WoodPalette build(ResourceManager rm, String ns, MaterialGroup group, TextureRole role,
	                                List<Candidate> all, NamespaceConfig nsCfg, double hueTolerance, double chromaGuard) {
		Palette raw = rawPalette(rm, ns, group, role, all, nsCfg);
		if (raw == null) return null;
		WoodPalette wp = WoodPalette.purified(raw, hueTolerance, chromaGuard);
		if (wp.palette().size() != raw.size()) {
			PaletteUnifierClient.LOGGER.debug("{} {}/{}: dropped {} non-wood colours from source palette",
					ns, group.name, role.key, raw.size() - wp.palette().size());
		}
		return wp;
	}

	private static Palette rawPalette(ResourceManager rm, String ns, MaterialGroup group, TextureRole role,
	                                  List<Candidate> all, NamespaceConfig nsCfg) {
		// 1. explicit override from config
		if (nsCfg != null && nsCfg.sourceTextures != null) {
			List<String> ids = nsCfg.sourceTextures.get(group.name);
			if (ids != null && !ids.isEmpty()) {
				List<Palette> ps = new ArrayList<>();
				for (String s : ids) {
					Palette p = paletteOf(rm, new Identifier(s));
					if (p != null) ps.add(p);
				}
				if (!ps.isEmpty()) return cap(merge(ps), ns, group, role);
			}
		}

		// 2. textures of this namespace+group with the wanted role (bark for logs, planks otherwise)
		List<Candidate> pool = new ArrayList<>();
		for (Candidate c : all) {
			if (c.group() == group && c.role() == role) pool.add(c);
		}
		if (pool.isEmpty()) return null;

		List<Candidate> picked = new ArrayList<>();
		if (role == TextureRole.PLANKS) {
			for (Pattern pref : PREFERRED) {
				for (Candidate c : pool) {
					if (pref.matcher(c.path()).find()) picked.add(c);
				}
				if (!picked.isEmpty()) break;
			}
		}
		List<Palette> palettes = new ArrayList<>();
		if (!picked.isEmpty()) {
			// Among the preferred names, the plainest (fewest colours) are the cleanest wood samples.
			List<Palette> sized = new ArrayList<>();
			for (Candidate c : picked.subList(0, Math.min(12, picked.size()))) {
				Palette p = paletteOf(rm, c.id());
				if (p != null && p.size() >= 2) sized.add(p);
			}
			sized.sort(Comparator.comparingInt(Palette::size));
			for (Palette p : sized.subList(0, Math.min(3, sized.size()))) palettes.add(p);
		}
		if (palettes.isEmpty()) {
			List<Palette> sized = new ArrayList<>();
			for (Candidate c : pool.subList(0, Math.min(24, pool.size()))) {
				Palette p = paletteOf(rm, c.id());
				if (p != null && p.size() >= 2) sized.add(p);
			}
			sized.sort(Comparator.comparingInt(Palette::size));
			for (Palette p : sized.subList(0, Math.min(3, sized.size()))) palettes.add(p);
		}
		if (palettes.isEmpty()) return null;
		return cap(merge(palettes), ns, group, role);
	}

	private static Palette merge(List<Palette> ps) {
		if (ps.size() == 1) return ps.get(0);
		return Palette.merge(ps.toArray(new Palette[0]));
	}

	private static Palette cap(Palette p, String ns, MaterialGroup group, TextureRole role) {
		int before = p.size();
		int guard = 64;
		while (p.size() > MAX_SOURCE_COLORS && guard-- > 0) {
			p.reduce();
		}
		if (before > MAX_SOURCE_COLORS * 2) {
			PaletteUnifierClient.LOGGER.debug("{} {}/{}: source palette had {} colours, reduced to {} (texture may not be pure wood)",
					ns, group.name, role.key, before, p.size());
		}
		return p;
	}

	private static Palette paletteOf(ResourceManager rm, Identifier id) {
		try (TextureImage img = TextureImage.open(rm, id)) {
			return Palette.fromImage(img);
		} catch (Exception e) {
			return null;
		}
	}
}
