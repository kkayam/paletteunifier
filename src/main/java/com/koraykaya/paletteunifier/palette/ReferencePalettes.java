package com.koraykaya.paletteunifier.palette;

import com.koraykaya.paletteunifier.PaletteUnifierClient;
import com.koraykaya.paletteunifier.model.MaterialGroup;
import com.koraykaya.paletteunifier.model.TextureRole;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Target palettes, read from the ACTIVE resource pack stack, memoised for one reload. */
public final class ReferencePalettes {
	private final ResourceManager rm;
	private final Map<String, Optional<Palette>> cache = new ConcurrentHashMap<>();
	private final Set<String> warned = ConcurrentHashMap.newKeySet();

	public ReferencePalettes(ResourceManager rm) {
		this.rm = rm;
	}

	/** Palette of the reference texture for group+role (following the role fallback chain). Null if none loads. */
	public Palette get(MaterialGroup group, TextureRole role) {
		TextureRole r = role;
		while (r != null) {
			Identifier ref = group.refs.get(r);
			if (ref != null) {
				Palette p = load(ref);
				if (p != null) return p;
			}
			r = r.fallback();
		}
		Identifier planks = group.refs.get(TextureRole.PLANKS);
		return planks == null ? null : load(planks);
	}

	private Palette load(Identifier ref) {
		return cache.computeIfAbsent(ref.toString(), k -> {
			try (TextureImage img = TextureImage.open(rm, ref)) {
				Palette p = Palette.fromImage(img);
				return Optional.of(p);
			} catch (Exception e) {
				if (warned.add(k)) {
					PaletteUnifierClient.LOGGER.warn("Reference texture {} not found or unreadable ({}); falling back", k, e.toString());
				}
				return Optional.empty();
			}
		}).orElse(null);
	}
}
