package com.koraykaya.paletteunifier.recolor;

import net.mehvahdjukaar.moonlight.api.resources.textures.Palette;
import net.mehvahdjukaar.moonlight.api.resources.textures.Respriter;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;

/** Whole-texture palette swap through Moonlight's Respriter. Right for textures that are nothing but wood. */
public final class FullRecolorer {
	private FullRecolorer() {}

	public static TextureImage recolor(TextureImage input, Palette target) {
		return Respriter.of(input).recolor(target);
	}
}
