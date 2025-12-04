package net.saint.createrenderfixer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Lightweight runtime-tweakable flags for Flywheel/Create instancing mitigations.
 *
 * These are intentionally kept simple (static + volatile) so they can be flipped from client
 * commands without any serialization layer.
 */
public final class ModConfig {

	// Properties

	private static volatile boolean cacheDynamicInstances = true;

	private static volatile boolean freezeDistantInstances = true;

	private static volatile int freezeBlockDistance = 62; // 64 - 2 buffer

	private static final Set<ResourceLocation> freezeBlacklist = ConcurrentHashMap.newKeySet();

	// Accessors

	public static boolean cacheDynamicInstances() {
		return cacheDynamicInstances;
	}

	public static boolean freezeDistantInstances() {
		return freezeDistantInstances;
	}

	public static int freezeBlockDistance() {
		return freezeBlockDistance;
	}

	public static java.util.Set<net.minecraft.resources.ResourceLocation> freezeBlacklist() {
		return java.util.Collections.unmodifiableSet(freezeBlacklist);
	}

	public static void setCacheDynamicInstances(boolean value) {
		cacheDynamicInstances = value;
		Mod.LOGGER.info("Instance data caching set to {}", value ? "ENABLED" : "DISABLED");
	}

	public static void setFreezeDistantInstances(boolean value) {
		freezeDistantInstances = value;
		Mod.LOGGER.info("Freezing distant instances set to {}", value ? "ENABLED" : "DISABLED");
	}

	public static void setFreezeBlockDistance(int blocks) {
		freezeBlockDistance = Math.max(0, blocks);
		Mod.LOGGER.info("Freeze distance set to {} blocks", freezeBlockDistance);
	}

	public static void addFreezeBlacklist(net.minecraft.resources.ResourceLocation id) {
		if (freezeBlacklist.add(id)) {
			Mod.LOGGER.info("Added {} to freeze blacklist", id);
		}
	}

	public static void removeFreezeBlacklist(net.minecraft.resources.ResourceLocation id) {
		if (freezeBlacklist.remove(id)) {
			Mod.LOGGER.info("Removed {} from freeze blacklist", id);
		}
	}

	public static void clearFreezeBlacklist() {
		freezeBlacklist.clear();
		Mod.LOGGER.info("Cleared freeze blacklist");
	}

	public static boolean isFreezeBlacklisted(net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
		var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
		return id != null && freezeBlacklist.contains(id);
	}

	// Debug

	public static String debugDescription() {
		return "cacheDynamicInstances=" + cacheDynamicInstances + ", freezeDistantInstances=" + freezeDistantInstances
				+ ", freezeDistanceBlocks=" + freezeBlockDistance + ", freezeBlacklist=" + freezeBlacklist;
	}
}
