package com.koraykaya.paletteunifier.recolor;

import com.koraykaya.paletteunifier.palette.SortedPalette;
import com.koraykaya.paletteunifier.palette.WoodPalette;
import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.PaletteColor;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-pixel conditional palette remap with three gates.
 *
 * <p>A pixel is replaced by the reference colour of the same luminance rank only if
 * (1) it is within {@code tolerance} (Lab distance) of the mod's own wood palette,
 * (2) its hue is within {@code hueTolerance} degrees of that palette's mean hue (moss is 60°+ away), and
 * (3) its chroma is at least {@code chromaGuard} × the chroma of the nearest wood colour (metal and
 * white highlights are near-grey) and at most 1.5 × the palette's most colourful tone (gold, paint).
 * Everything else, handles, glass, cushions, nails, moss, stays untouched.
 * Rank mapping keeps the mod's light/dark structure and only borrows hue and temperature.
 */
public final class ConditionalRecolorer {
	private ConditionalRecolorer() {}

	public static TextureImage recolor(TextureImage input, WoodPalette source, Palette target,
	                                   double tolerance, double hueTolerance, double chromaGuard) {
		SortedPalette src = new SortedPalette(source.palette());
		if (src.size() == 0) return input.makeCopy();
		SortedPalette dst = SortedPalette.resized(target, src.size());
		int n = Math.min(src.size(), dst.size());
		int[] mapped = new int[src.size()];
		for (int i = 0; i < src.size(); i++) {
			int j = Math.min(i, n - 1);
			mapped[i] = dst.colors[Math.max(0, j)].value();
		}
		float meanHue = source.meanHue();
		float maxChroma = Math.max(source.maxChroma() * 1.5f, WoodPalette.GREY_CHROMA * 4);
		float[] srcChroma = new float[src.size()];
		for (int i = 0; i < src.size(); i++) srcChroma[i] = WoodPalette.chromaOf(src.colors[i]);

		Map<Integer, Integer> lut = new HashMap<>();
		TextureImage out = input.makeCopy();
		int w = input.imageWidth(), h = input.imageHeight();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int px = input.getPixel(x, y);
				int alpha = px >>> 24;
				if (alpha == 0) continue;
				int rgb = px & 0x00FFFFFF;
				Integer res = lut.get(rgb);
				if (res == null) {
					res = rgb;
					PaletteColor pc = new PaletteColor(px | 0xFF000000);
					float chroma = WoodPalette.chromaOf(pc);
					if (chroma >= WoodPalette.GREY_CHROMA && chroma <= maxChroma
							&& WoodPalette.hueDiff(WoodPalette.hueOf(pc), meanHue) <= hueTolerance) {
						int best = -1;
						float bestD = Float.MAX_VALUE;
						for (int i = 0; i < src.size(); i++) {
							float d = src.colors[i].distanceTo(pc);
							if (d < bestD) {
								bestD = d;
								best = i;
							}
						}
						if (best >= 0 && bestD <= tolerance && chroma >= chromaGuard * srcChroma[best]) {
							res = mapped[best] & 0x00FFFFFF;
						}
					}
					lut.put(rgb, res);
				}
				out.setPixel(x, y, (alpha << 24) | res);
			}
		}
		return out;
	}
}
