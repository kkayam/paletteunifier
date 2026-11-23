package com.koraykaya.paletteunifier.command;

import com.koraykaya.paletteunifier.PaletteUnifierClient;
import com.koraykaya.paletteunifier.PaletteUnifierGenerator;
import com.koraykaya.paletteunifier.config.ConfigLoader;
import com.koraykaya.paletteunifier.io.DebugExporter;
import com.koraykaya.paletteunifier.palette.PaletteCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Map;

/** /paletteunifier on|off | reload | stats | debug on|off | export <namespace> */
public final class PaletteUnifierCommands {
	private PaletteUnifierCommands() {}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommandManager.literal("paletteunifier")
						.then(ClientCommandManager.literal("reload").executes(ctx -> {
							ctx.getSource().sendFeedback(Text.literal("[Palette Unifier] reloading resources..."));
							MinecraftClient.getInstance().reloadResources();
							return 1;
						}))
						.then(ClientCommandManager.literal("stats").executes(ctx -> {
							PaletteUnifierGenerator g = PaletteUnifierGenerator.INSTANCE;
							if (g == null) {
								ctx.getSource().sendFeedback(Text.literal("[Palette Unifier] not initialised"));
								return 0;
							}
							for (String line : g.lastStats().lines()) {
								ctx.getSource().sendFeedback(Text.literal(line));
							}
							ctx.getSource().sendFeedback(Text.literal("cache entries: " + g.cache().size()));
							return 1;
						}))
						.then(ClientCommandManager.literal("on").executes(ctx -> setEnabled(ctx.getSource(), true)))
						.then(ClientCommandManager.literal("off").executes(ctx -> setEnabled(ctx.getSource(), false)))
						.then(ClientCommandManager.literal("debug")
								.then(ClientCommandManager.literal("on").executes(ctx -> setDebug(ctx.getSource(), true)))
								.then(ClientCommandManager.literal("off").executes(ctx -> setDebug(ctx.getSource(), false))))
						.then(ClientCommandManager.literal("export")
								.then(ClientCommandManager.argument("namespace", StringArgumentType.word()).executes(ctx -> {
									String ns = StringArgumentType.getString(ctx, "namespace");
									return export(ctx.getSource(), ns);
								})))
		));
	}

	private static int setEnabled(FabricClientCommandSource src, boolean on) {
		PaletteUnifierClient.CONFIG.enabled = on;
		ConfigLoader.save(PaletteUnifierClient.CONFIG);
		src.sendFeedback(Text.literal("[Palette Unifier] " + (on ? "enabled" : "disabled") + ", reloading resources..."));
		MinecraftClient.getInstance().reloadResources();
		return 1;
	}

	private static int setDebug(FabricClientCommandSource src, boolean on) {
		PaletteUnifierClient.CONFIG.debugExport = on;
		ConfigLoader.save(PaletteUnifierClient.CONFIG);
		src.sendFeedback(Text.literal("[Palette Unifier] debugExport = " + on + " (saved; takes effect on next reload)"));
		return 1;
	}

	@SuppressWarnings("unchecked")
	private static int export(FabricClientCommandSource src, String ns) {
		PaletteUnifierGenerator g = PaletteUnifierGenerator.INSTANCE;
		if (g == null) return 0;
		int n = 0;
		try {
			Field f = PaletteCache.class.getDeclaredField("map");
			f.setAccessible(true);
			Map<net.minecraft.util.Identifier, PaletteCache.Entry> map = (Map<net.minecraft.util.Identifier, PaletteCache.Entry>) f.get(g.cache());
			for (Map.Entry<net.minecraft.util.Identifier, PaletteCache.Entry> e : map.entrySet()) {
				if (!e.getKey().getNamespace().equals(ns)) continue;
				try (var img = PaletteCache.toImage(e.getValue())) {
					DebugExporter.exportOne("export", e.getKey(), img);
					n++;
				}
			}
		} catch (Exception ex) {
			src.sendFeedback(Text.literal("[Palette Unifier] export failed: " + ex));
			return 0;
		}
		src.sendFeedback(Text.literal("[Palette Unifier] exported " + n + " textures to " + ConfigLoader.DIR.resolve("export").resolve(ns)));
		return 1;
	}
}
