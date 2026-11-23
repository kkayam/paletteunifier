package com.koraykaya.paletteunifier.palette;

import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.PaletteColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** An immutable, luminance-sorted view of a Moonlight palette, plus a stable hash. */
public final class SortedPalette {
	public final PaletteColor[] colors;   // dark -> light
	public final long hash;

	public SortedPalette(Palette palette) {
		List<PaletteColor> list = new ArrayList<>(palette.getValues());
		list.sort(Comparator.comparingDouble(PaletteColor::luminance));
		this.colors = list.toArray(new PaletteColor[0]);
		long h = 1469598103934665603L;
		for (PaletteColor c : colors) {
			h ^= c.value();
			h *= 1099511628211L;
		}
		this.hash = h;
	}

	public int size() {
		return colors.length;
	}

	/** A copy of the underlying palette resized to {@code n} colours, luminance-sorted. */
	public static SortedPalette resized(Palette source, int n) {
		Palette copy = source.copy();
		if (n > 0 && copy.size() != n) {
			copy.matchSize(n);
		}
		return new SortedPalette(copy);
	}
}
