package com.koraykaya.paletteunifier;

import com.koraykaya.paletteunifier.command.PaletteUnifierCommands;
import com.koraykaya.paletteunifier.config.ConfigLoader;
import com.koraykaya.paletteunifier.config.PaletteUnifierConfig;
import net.fabricmc.api.ClientModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PaletteUnifierClient implements ClientModInitializer {
	public static final String MOD_ID = "paletteunifier";
	public static final Logger LOGGER = LogManager.getLogger("PaletteUnifier");
	public static volatile PaletteUnifierConfig CONFIG = PaletteUnifierConfig.defaults();

	@Override
	public void onInitializeClient() {
		CONFIG = ConfigLoader.loadOrCreate();
		// Constructing the generator registers it with Moonlight; it runs on every resource reload.
		new PaletteUnifierGenerator();
		PaletteUnifierCommands.register();
		LOGGER.info("Palette Unifier ready: {} material groups, {} namespaces listed",
				CONFIG.groups.size(), CONFIG.knownNamespaces.size());
	}
}
