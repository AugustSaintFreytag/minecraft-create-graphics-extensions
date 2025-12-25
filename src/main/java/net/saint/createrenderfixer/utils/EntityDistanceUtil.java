package net.saint.createrenderfixer.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class EntityDistanceUtil {

	private static final double MAX_DISTANCE_FACTOR = 64.0;

	public static boolean shouldRenderAtSqrDistance(Entity entity, double distance) {
		var maxDistance = getMaxDistanceSqr(entity);
		return distance < maxDistance;
	}

	public static double getMaxDistanceSqr(Entity entity) {
		var size = getSize(entity);

		var maxDistance = size * MAX_DISTANCE_FACTOR * Entity.getViewScale();
		maxDistance = getMaxDistanceForWorld(maxDistance);

		return maxDistance * maxDistance;
	}

	public static double getSize(Entity entity) {
		var size = entity.getBoundingBox().getSize();

		if (Double.isNaN(size)) {
			return 1.0;
		}

		return Math.min(size, 1.0);
	}

	private static double getMaxDistanceForWorld(double maxDistance) {
		var chunkDistance = getMaxUnboundedDistanceForWorld();
		return Math.min(maxDistance, chunkDistance);
	}

	public static int getMaxUnboundedDistanceForWorld() {
		var client = Minecraft.getInstance();

		if (client == null || client.options == null) {
			return 0;

		}

		var chunkDistance = client.options.getEffectiveRenderDistance() * 16;
		return chunkDistance;
	}

}
