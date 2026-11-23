package com.koraykaya.paletteunifier.palette;

import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.PaletteColor;

import java.util.ArrayList;
import java.util.List;

/**
 * A mod's "this is what our oak looks like" palette plus the hue/chroma statistics the conditional
 * gate needs. Hue is in degrees (0..360), chroma in Moonlight's Lab units (sqrt(a²+b²)).
 */
public record WoodPalette(Palette palette, float meanHue, float minChroma, float medianChroma, float maxChroma) {

	/** Chroma below this is treated as grey: hue is meaningless and such pixels are never wood. */
	public static final float GREY_CHROMA = 0.012f;

	public static float hueOf(PaletteColor c) {
		var lab = c.lab();
		double h = Math.toDegrees(Math.atan2(lab.b(), lab.a()));
		return (float) (h < 0 ? h + 360 : h);
	}

	public static float chromaOf(PaletteColor c) {
		var lab = c.lab();
		return (float) Math.hypot(lab.a(), lab.b());
	}

	/** Smallest angular difference between two hues in degrees. */
	public static float hueDiff(float a, float b) {
		float d = Math.abs(a - b) % 360f;
		return d > 180f ? 360f - d : d;
	}

	/** Statistics only; no purification. */
	public static WoodPalette of(Palette p) {
		List<PaletteColor> cols = new ArrayList<>(p.getValues());
		return stats(p, cols);
	}

	/**
	 * Drops colours that are not wood-like relative to the rest of the palette: hue further than
	 * {@code hueTolerance} degrees from the chroma-weighted mean hue, or chroma below
	 * {@code chromaGuard} × median chroma. Falls back to the unpurified palette if fewer than two
	 * colours would remain.
	 */
	public static WoodPalette purified(Palette p, double hueTolerance, double chromaGuard) {
		List<PaletteColor> cols = new ArrayList<>(p.getValues());
		if (cols.size() < 3) return stats(p, cols);
		WoodPalette raw = stats(p, cols);
		List<PaletteColor> keep = new ArrayList<>();
		for (PaletteColor c : cols) {
			float ch = chromaOf(c);
			if (ch < GREY_CHROMA) continue;
			if (ch < chromaGuard * raw.medianChroma) continue;
			if (hueDiff(hueOf(c), raw.meanHue) > hueTolerance) continue;
			keep.add(c);
		}
		if (keep.size() < 2 || keep.size() == cols.size()) return raw;
		Palette np = Palette.ofColors(keep.stream().map(PaletteColor::rgb).toList());
		return stats(np, keep);
	}

	private static WoodPalette stats(Palette p, List<PaletteColor> cols) {
		double sx = 0, sy = 0;
		List<Float> chromas = new ArrayList<>();
		float min = Float.MAX_VALUE, max = 0;
		for (PaletteColor c : cols) {
			float ch = chromaOf(c);
			float h = hueOf(c);
			if (ch > max) max = ch;
			double w = Math.max(ch, 1e-4);
			sx += w * Math.cos(Math.toRadians(h));
			sy += w * Math.sin(Math.toRadians(h));
			chromas.add(ch);
			if (ch < min) min = ch;
		}
		double mh = Math.toDegrees(Math.atan2(sy, sx));
		if (mh < 0) mh += 360;
		chromas.sort(Float::compare);
		float median = chromas.isEmpty() ? 0 : chromas.get(chromas.size() / 2);
		if (min == Float.MAX_VALUE) min = 0;
		return new WoodPalette(p, (float) mh, min, median, max);
	}
}
