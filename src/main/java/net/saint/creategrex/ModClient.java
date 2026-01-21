package net.saint.creategrex;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.saint.creategrex.client.WindmillLODRenderManager;
import net.saint.creategrex.dh.WindmillLODManager;
import net.saint.creategrex.dh.WindmillLODMaterialManager;
import net.saint.creategrex.network.WindmillLODSyncUtil;

public final class ModClient implements ClientModInitializer {

	// State

	public static WindmillLODManager WINDMILL_LOD_MANAGER;
	public static WindmillLODMaterialManager WINDMILL_LOD_MATERIAL_MANAGER;
	public static WindmillLODRenderManager WINDMILL_LOD_RENDER_MANAGER;

	// Init

	@Override
	public void onInitializeClient() {
		ModClientCommands.init();

		if (FabricLoader.getInstance().isModLoaded("distanthorizons")) {
			initializeDistantHorizonsInterop();
		}
	}

	private void initializeDistantHorizonsInterop() {
		WINDMILL_LOD_MANAGER = new WindmillLODManager();
		WINDMILL_LOD_MATERIAL_MANAGER = new WindmillLODMaterialManager();
		WINDMILL_LOD_RENDER_MANAGER = new WindmillLODRenderManager();

		reloadWindmillMaterialManagerFromConfig();
		registerConfigReloadListener();

		WindmillLODSyncUtil.initClient();

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (WINDMILL_LOD_RENDER_MANAGER == null) {
				return;
			}

			WINDMILL_LOD_RENDER_MANAGER.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			var level = client.level;

			if (level == null) {
				return;
			}

			var renderManager = WINDMILL_LOD_RENDER_MANAGER;

			if (renderManager == null) {
				return;
			}

			var partialTicks = Minecraft.getInstance().getFrameTime();
			renderManager.tick(level, partialTicks);
		});
	}

	// Config

	private static void registerConfigReloadListener() {
		AutoConfig.getConfigHolder(ModConfig.class).registerSaveListener((config, data) -> {
			reloadWindmillMaterialManagerFromConfig();
			return null;
		});
	}

	private static void reloadWindmillMaterialManagerFromConfig() {
		if (WINDMILL_LOD_MATERIAL_MANAGER == null) {
			return;
		}

		WINDMILL_LOD_MATERIAL_MANAGER.reloadFromConfig();
	}
}
