package net.saint.creategrex;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiLevelUnloadEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

import io.github.fabricators_of_create.porting_lib.event.client.ClientWorldEvents;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.saint.creategrex.client.WindmillLODRenderManager;
import net.saint.creategrex.dh.WindmillLODManager;
import net.saint.creategrex.dh.WindmillLODMaterialManager;
import net.saint.creategrex.network.WindmillLODSyncUtil;

public final class ModClient implements ClientModInitializer {

	// State

	public static WindmillLODManager WINDMILL_LOD_MANAGER;
	public static WindmillLODMaterialManager WINDMILL_LOD_MATERIAL_MANAGER;
	public static WindmillLODRenderManager WINDMILL_LOD_RENDER_MANAGER;

	public static boolean isDHRendererInitialized = false;

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

		ClientWorldEvents.UNLOAD.register((client, level) -> {
			reloadWindmillMaterialManagerFromConfig();
			clearWindmillLODRenderManager();

			isDHRendererInitialized = false;
		});

		DhApi.events.bind(DhApiLevelLoadEvent.class, new DhApiLevelLoadEvent() {
			@Override
			public void onLevelLoad(DhApiEventParam<EventParam> param) {
				if (!isDHRendererInitialized) {
					clearWindmillLODRenderManager();
					isDHRendererInitialized = true;

					Mod.LOGGER.info("Clearing render references for animated LODs on DH level load.");
				}
			}
		});

		DhApi.events.bind(DhApiLevelUnloadEvent.class, new DhApiLevelUnloadEvent() {
			@Override
			public void onLevelUnload(DhApiEventParam<EventParam> param) {
				isDHRendererInitialized = false;
				Mod.LOGGER.info("Clearing render references for animated LODs and rearming on DH level unload.");
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			var level = client.level;

			if (level == null) {
				return;
			}

			if (WINDMILL_LOD_RENDER_MANAGER == null) {
				return;
			}

			var partialTicks = client.getFrameTime();
			WINDMILL_LOD_RENDER_MANAGER.tick(level, partialTicks);
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

	private static void clearWindmillLODRenderManager() {
		if (WINDMILL_LOD_RENDER_MANAGER == null) {
			return;
		}

		WINDMILL_LOD_RENDER_MANAGER.clear();
	}
}
