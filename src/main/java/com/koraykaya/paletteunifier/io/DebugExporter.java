package com.koraykaya.paletteunifier.io;

import com.koraykaya.paletteunifier.PaletteUnifierClient;
import com.koraykaya.paletteunifier.config.ConfigLoader;
import net.mehvahdjukaar.moonlight.api.resources.textures.TextureImage;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** Writes before/after PNGs under config/paletteunifier/debug/<ns>/<path>.(before|after).png */
public final class DebugExporter {
	private static final AtomicInteger written = new AtomicInteger();

	private DebugExporter() {}

	public static void resetCounter() {
		written.set(0);
	}

	public static void export(Identifier id, TextureImage before, TextureImage after, int max) {
		if (written.get() >= max) return;
		try {
			Path base = ConfigLoader.DIR.resolve("debug").resolve(id.getNamespace()).resolve(id.getPath());
			Files.createDirectories(base.getParent());
			String name = base.getFileName().toString();
			before.getImage().writeTo(base.resolveSibling(name + ".before.png"));
			after.getImage().writeTo(base.resolveSibling(name + ".after.png"));
			written.incrementAndGet();
		} catch (Exception e) {
			PaletteUnifierClient.LOGGER.warn("Debug export failed for {}: {}", id, e.toString());
		}
	}

	/** Export a single generated texture (used by /paletteunifier export). */
	public static Path exportOne(String folder, Identifier id, TextureImage img) throws Exception {
		Path base = ConfigLoader.DIR.resolve(folder).resolve(id.getNamespace()).resolve(id.getPath() + ".png");
		Files.createDirectories(base.getParent());
		img.getImage().writeTo(base);
		return base;
	}
}
