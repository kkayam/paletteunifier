package com.koraykaya.paletteunifier.model;

import net.minecraft.util.Identifier;

/**
 * A texture that will be recolored.
 *
 * @param id    texture id without the "textures/" prefix and ".png" suffix, e.g. handcrafted:block/bench/oak_bench
 * @param group material group it belongs to
 * @param role  which reference it is matched against
 */
public record Candidate(Identifier id, MaterialGroup group, TextureRole role) {
	public String path() {
		return id.getPath();
	}
}
