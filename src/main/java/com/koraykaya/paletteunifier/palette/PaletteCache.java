package com.koraykaya.paletteunifier.palette;

import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers generated pixels so a reload with unchanged inputs only re-emits instead of recomputing. */
public final class PaletteCache {
	public record Entry(long inputHash, long targetHash, int width, int height, int[] pixels) {}

	private final Map<Identifier, Entry> map = new ConcurrentHashMap<>();
	private volatile int max;

	public PaletteCache(int max) {
		this.max = max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public Entry get(Identifier id, long inputHash, long targetHash) {
		Entry e = map.get(id);
		if (e != null && e.inputHash == inputHash && e.targetHash == targetHash) return e;
		return null;
	}

	public void put(Identifier id, long inputHash, long targetHash, TextureImage out) {
		if (map.size() >= max) map.clear();
		map.put(id, new Entry(inputHash, targetHash, out.imageWidth(), out.imageHeight(), pixels(out)));
	}

	public void clear() {
		map.clear();
	}

	public int size() {
		return map.size();
	}

	public static TextureImage toImage(Entry e) {
		TextureImage img = TextureImage.createNew(e.width, e.height);
		int i = 0;
		for (int y = 0; y < e.height; y++) {
			for (int x = 0; x < e.width; x++) {
				img.setPixel(x, y, e.pixels[i++]);
			}
		}
		return img;
	}

	public static int[] pixels(TextureImage img) {
		int w = img.imageWidth(), h = img.imageHeight();
		int[] px = new int[w * h];
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				px[i++] = img.getPixel(x, y);
			}
		}
		return px;
	}

	public static long hash(TextureImage img) {
		long h = 1469598103934665603L;
		int w = img.imageWidth(), hh = img.imageHeight();
		h ^= w; h *= 1099511628211L;
		h ^= hh; h *= 1099511628211L;
		for (int y = 0; y < hh; y++) {
			for (int x = 0; x < w; x++) {
				h ^= img.getPixel(x, y);
				h *= 1099511628211L;
			}
		}
		return h;
	}
}
