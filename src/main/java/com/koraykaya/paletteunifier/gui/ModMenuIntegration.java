package com.koraykaya.paletteunifier.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Mod Menu entrypoint. The screen itself needs Cloth Config; without it we show a short notice. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return parent -> ConfigScreenBuilder.build(parent);
		}
		return NoClothScreen::new;
	}

	/** Fallback when Cloth Config is missing. */
	static final class NoClothScreen extends Screen {
		private final Screen parent;

		NoClothScreen(Screen parent) {
			super(Text.literal("Palette Unifier"));
			this.parent = parent;
		}

		@Override
		protected void init() {
			addDrawableChild(net.minecraft.client.gui.widget.ButtonWidget.builder(Text.literal("Back"), b -> close())
					.dimensions(width / 2 - 50, height / 2 + 20, 100, 20).build());
		}

		@Override
		public void render(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
			renderBackground(ctx);
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Install Cloth Config to edit settings in-game."), width / 2, height / 2 - 20, 0xFFFFFF);
			ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Or edit config/paletteunifier.json5 and press F3+T."), width / 2, height / 2 - 6, 0xAAAAAA);
			super.render(ctx, mouseX, mouseY, delta);
		}

		@Override
		public void close() {
			client.setScreen(parent);
		}
	}
}
