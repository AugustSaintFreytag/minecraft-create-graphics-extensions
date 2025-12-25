package net.saint.createrenderfixer.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiChunkModifiedEvent;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.saint.createrenderfixer.Mod;

/**
 * Thread-safe registry of contraption blocks keyed by dimension + chunk for DH overrides.
 */
public final class ContraptionBlockRegistry {

	// Library (Records)

	public record StoredBlock(int x, int y, int z, BlockState state, String biomeId) {
	}

	public record LookupResult(BlockState state, @Nullable
	String biomeId) {
	}

	// Library (Models)

	private static final class BlockPosState {
		final int x;
		final int y;
		final int z;
		final BlockState state;
		final String biomeId;

		BlockPosState(BlockPos pos, BlockState state, String biomeId) {
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			this.state = state;
			this.biomeId = biomeId;
		}

		boolean matches(int x, int y, int z) {
			return this.x == x && this.y == y && this.z == z;
		}
	}

	private static final class ContraptionEntry {
		final String dimensionId;
		final Map<Long, List<BlockPosState>> chunks = new ConcurrentHashMap<>();

		ContraptionEntry(String dimensionId) {
			this.dimensionId = dimensionId;
		}
	}

	// State

	private static final Map<UUID, ContraptionEntry> CONTRAPTIONS = new ConcurrentHashMap<>();
	private static final Map<String, Map<Long, List<BlockPosState>>> BY_DIMENSION = new ConcurrentHashMap<>();

	// Registration

	public static void register(AbstractContraptionEntity entity) {
		if (!(entity.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		var contraption = entity.getContraption();

		if (contraption == null || contraption.getBlocks().isEmpty()) {
			return;
		}

		if (CONTRAPTIONS.containsKey(entity.getUUID())) {
			return;
		}

		var dimensionId = serverLevel.dimension().location().toString();
		var entry = new ContraptionEntry(dimensionId);
		var blockPosition = contraption.anchor;

		contraption.getBlocks().forEach((localPosition, info) -> {

			if (blockPosition == null) {
				Mod.LOGGER.warn("Contraption '{}' has no anchor block position and can not be registered.", entity.getUUID());
				return;
			}

			var worldPosition = localPosition.offset(blockPosition);

			if (worldPosition == null) {
				Mod.LOGGER.warn("Contraption '{}' has invalid block position and can not be registered.", entity.getUUID());
				return;
			}

			var biomeId = resolveBiomeId(serverLevel, worldPosition);
			var chunkPosition = new ChunkPos(blockPosition);
			var chunkKey = chunkPosition.toLong();

			entry.chunks.computeIfAbsent(chunkKey, key -> new ArrayList<>()).add(new BlockPosState(worldPosition, info.state(), biomeId));
		});

		CONTRAPTIONS.put(entity.getUUID(), entry);
		BY_DIMENSION.computeIfAbsent(dimensionId, key -> new ConcurrentHashMap<>()).putAll(entry.chunks);

		notifyChunksDirty(dimensionId, entry.chunks.keySet());
	}

	public static void unregister(UUID contraptionId) {
		var removed = CONTRAPTIONS.remove(contraptionId);

		if (removed == null) {
			return;
		}

		var chunkMap = BY_DIMENSION.get(removed.dimensionId);

		if (chunkMap == null) {
			return;
		}

		for (var chunkKey : removed.chunks.keySet()) {
			chunkMap.remove(chunkKey);
		}

		notifyChunksDirty(removed.dimensionId, removed.chunks.keySet());
	}

	public static void clearForWorld(String dimensionId) {
		BY_DIMENSION.remove(dimensionId);
		CONTRAPTIONS.entrySet().removeIf(e -> e.getValue().dimensionId.equals(dimensionId));
	}

	@Nullable
	public static LookupResult find(String dimensionId, int worldX, int worldY, int worldZ) {
		var chunkMap = resolveChunkMap(dimensionId);

		if (chunkMap == null) {
			return null;
		}

		var chunkPosition = new ChunkPos(new BlockPos(worldX, worldY, worldZ));
		var chunkKey = chunkPosition.toLong();
		var entries = chunkMap.get(chunkKey);

		if (entries == null) {
			return null;
		}

		for (var entry : entries) {
			if (entry.matches(worldX, worldY, worldZ)) {
				return new LookupResult(entry.state, entry.biomeId);
			}
		}

		return null;
	}

	public static int highestYInColumn(String dimensionId, int worldX, int worldZ, int defaultHeight) {
		var chunkMap = resolveChunkMap(dimensionId);

		if (chunkMap == null) {
			return defaultHeight;
		}

		var chunkKey = ChunkPos.asLong(new BlockPos(worldX, 0, worldZ));
		var entries = chunkMap.get(chunkKey);

		if (entries == null || entries.isEmpty()) {
			return defaultHeight;
		}

		var maxY = Integer.MIN_VALUE;

		for (var entry : entries) {
			if (entry.x == worldX && entry.z == worldZ) {
				if (entry.y > maxY) {
					maxY = entry.y;
				}
			}
		}

		if (maxY == Integer.MIN_VALUE) {
			return defaultHeight;
		}

		return Math.max(defaultHeight, maxY);
	}

	// Management

	public static Map<String, Map<Long, List<StoredBlock>>> snapshot() {
		var result = new ConcurrentHashMap<String, Map<Long, List<StoredBlock>>>();

		BY_DIMENSION.forEach((dimensionId, chunkMap) -> {
			var chunkCopy = new ConcurrentHashMap<Long, List<StoredBlock>>();

			chunkMap.forEach((chunkKey, entries) -> {
				var list = new ArrayList<StoredBlock>(entries.size());
				for (var entry : entries) {
					list.add(new StoredBlock(entry.x, entry.y, entry.z, entry.state, entry.biomeId));
				}
				chunkCopy.put(chunkKey, list);
			});

			result.put(dimensionId, chunkCopy);
		});

		return result;
	}

	public static void loadPersistent(Map<String, Map<Long, List<StoredBlock>>> data) {
		BY_DIMENSION.clear();

		for (var entry : data.entrySet()) {
			var chunkMap = new ConcurrentHashMap<Long, List<BlockPosState>>();

			for (var chunkEntry : entry.getValue().entrySet()) {
				var list = new ArrayList<BlockPosState>();
				for (var block : chunkEntry.getValue()) {
					list.add(new BlockPosState(new BlockPos(block.x(), block.y(), block.z()), block.state(), block.biomeId()));
				}

				chunkMap.put(chunkEntry.getKey(), list);
			}

			BY_DIMENSION.put(entry.getKey(), chunkMap);
			notifyChunksDirty(entry.getKey(), chunkMap.keySet());
		}
	}

	private static void notifyChunksDirty(String dimensionId, Iterable<Long> chunkKeys) {
		if (!DhBridge.isReady()) {
			return;
		}

		var levelWrapper = resolveLevelWrapper(dimensionId);

		if (levelWrapper == null) {
			return;
		}

		for (var chunkKey : chunkKeys) {
			var chunkPos = new ChunkPos(chunkKey);
			try {
				DhApi.events.fireAllEvents(DhApiChunkModifiedEvent.class,
						new DhApiChunkModifiedEvent.EventParam(levelWrapper, chunkPos.x, chunkPos.z));
			} catch (Exception ignored) {
				// DH may not be ready to process the notification; ignore.
			}
		}
	}

	// Utility

	@Nullable
	private static Map<Long, List<BlockPosState>> resolveChunkMap(String dimensionId) {
		var chunkMap = BY_DIMENSION.get(dimensionId);
		if (chunkMap != null) {
			return chunkMap;
		}
		// Fallback: try dimension name suffix match (e.g., "minecraft:overworld" vs "overworld")
		for (var entry : BY_DIMENSION.entrySet()) {
			if (entry.getKey().endsWith(dimensionId) || dimensionId.endsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	private static String resolveBiomeId(ServerLevel level, BlockPos pos) {
		if (pos == null) {
			return "minecraft:plains";
		}

		Holder<Biome> biome = level.getBiome(pos);
		return biome.unwrapKey().map(key -> key.location().toString()).orElse("minecraft:plains");
	}

	@Nullable
	private static IDhApiLevelWrapper resolveLevelWrapper(String dimensionId) {
		var worldProxy = DhBridge.worldProxy();

		if (worldProxy == null) {
			return null;
		}

		try {
			for (var wrapper : worldProxy.getAllLoadedLevelWrappers()) {
				if (dimensionId.equals(wrapper.getDhIdentifier()) || dimensionId.endsWith(wrapper.getDimensionName())) {
					return wrapper;
				}
			}
		} catch (Exception ignored) {
			return null;
		}

		return null;
	}
}
