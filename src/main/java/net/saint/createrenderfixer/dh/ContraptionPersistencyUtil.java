package net.saint.createrenderfixer.dh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.saint.createrenderfixer.Mod;

/**
 * Persists contraption block snapshots so DH overrides survive restarts.
 */
public final class ContraptionPersistencyUtil {

	// Configuration

	private static final String FILE_NAME = "contraptions.nbt";

	// Load

	public static void load(MinecraftServer server) {
		var savePath = getSavePath(server);

		if (!Files.exists(savePath)) {
			return;
		}

		try {
			var root = NbtIo.readCompressed(savePath.toFile());
			var data = decode(server.registryAccess().lookupOrThrow(Registries.BLOCK), root);
			ContraptionBlockRegistry.loadPersistent(data.chunkData());
			WindmillLODManager.loadPersistent(data.windmills());

			var blockCount = countEntries(data.chunkData());
			var windmillCount = data.windmills().size();

			Mod.LOGGER.info("Loaded {} contraption blocks and {} windmill entries for DH overrides from file.", blockCount, windmillCount);
		} catch (Exception exception) {
			Mod.LOGGER.warn("Could not load contraption persistence data from file.", exception);
		}
	}

	// Save

	public static void save(MinecraftServer server) {
		var savePath = getSavePath(server);

		if (savePath == null) {
			Mod.LOGGER.error("Cannot save contraption persistence data, save path is not set.");
			return;
		}

		try {
			Files.createDirectories(savePath.getParent());

			var snapshot = ContraptionBlockRegistry.snapshot();
			var windmills = WindmillLODManager.snapshotPersistent();
			var tag = encode(snapshot, windmills);

			NbtIo.writeCompressed(tag, savePath.toFile());
		} catch (Exception exception) {
			Mod.LOGGER.error("Could not save contraption persistence data to file.", exception);
		}
	}

	private static Path getSavePath(MinecraftServer server) {
		return server.getServerDirectory().toPath().resolve("create-render-fixer").resolve(FILE_NAME);
	}

	// Encoding

	private static CompoundTag encode(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data,
			List<WindmillLODEntry> windmills) {
		var root = new CompoundTag();
		var dimensions = new ListTag();

		data.forEach((dimensionId, chunkMap) -> {
			var dimensionNBT = new CompoundTag();
			dimensionNBT.putString("id", dimensionId);

			var chunks = new ListTag();
			chunkMap.forEach((chunkKey, blocks) -> {
				var chunkTag = new CompoundTag();
				chunkTag.putLong("key", chunkKey);

				var list = new ListTag();
				for (var block : blocks) {
					var blockNBT = new CompoundTag();
					blockNBT.putInt("x", block.x());
					blockNBT.putInt("y", block.y());
					blockNBT.putInt("z", block.z());
					blockNBT.put("state", NbtUtils.writeBlockState(block.state()));

					if (block.biomeId() != null) {
						blockNBT.putString("biome", block.biomeId());
					}

					list.add(blockNBT);
				}

				chunkTag.put("blocks", list);
				chunks.add(chunkTag);
			});

			dimensionNBT.put("chunks", chunks);
			dimensions.add(dimensionNBT);
		});

		root.put("dims", dimensions);
		root.put("windmills", encodeWindmills(windmills));

		return root;
	}

	private static ListTag encodeWindmills(List<WindmillLODEntry> windmills) {
		var list = new ListTag();

		if (windmills == null || windmills.isEmpty()) {
			return list;
		}

		for (var entry : windmills) {
			if (entry == null) {
				continue;
			}

			var contraptionIdentifier = entry.contraptionId();
			var dimensionIdentifier = entry.dimensionId();
			var anchorPosition = entry.anchorPosition();
			var rotationAxis = entry.rotationAxis();

			if (contraptionIdentifier == null || dimensionIdentifier == null || anchorPosition == null || rotationAxis == null) {
				continue;
			}

			var windmillTag = new CompoundTag();
			windmillTag.putString("identifier", contraptionIdentifier.toString());
			windmillTag.putString("dimension", dimensionIdentifier);
			windmillTag.put("anchor", NbtUtils.writeBlockPos(anchorPosition));
			windmillTag.putString("axis", rotationAxis.getName());
			windmillTag.putFloat("speed", entry.rotationSpeed());
			windmillTag.putFloat("angle", entry.rotationAngle());
			windmillTag.putLong("lastSynchronizationTick", entry.lastSynchronizationTick());
			list.add(windmillTag);
		}

		return list;
	}

	// Decoding

	private static PersistedData decode(HolderGetter<Block> blocks, @Nullable
	CompoundTag root) throws IOException {
		var data = new HashMap<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>>();
		var windmills = decodeWindmills(root);

		if (root == null || !root.contains("dims", Tag.TAG_LIST)) {
			return new PersistedData(data, windmills);
		}

		for (Tag dimTagRaw : root.getList("dims", Tag.TAG_COMPOUND)) {
			if (!(dimTagRaw instanceof CompoundTag dimTag)) {
				continue;
			}

			var id = dimTag.getString("id");
			var chunkMap = new HashMap<Long, List<ContraptionBlockRegistry.StoredBlock>>();

			for (var chunkRaw : dimTag.getList("chunks", Tag.TAG_COMPOUND)) {
				if (!(chunkRaw instanceof CompoundTag chunkTag)) {
					continue;
				}

				var key = chunkTag.getLong("key");
				var list = new ArrayList<ContraptionBlockRegistry.StoredBlock>();

				for (var blockRaw : chunkTag.getList("blocks", Tag.TAG_COMPOUND)) {
					if (!(blockRaw instanceof CompoundTag blockTag)) {
						continue;
					}

					var state = NbtUtils.readBlockState(blocks, blockTag.getCompound("state"));
					var x = blockTag.getInt("x");
					var y = blockTag.getInt("y");
					var z = blockTag.getInt("z");
					var biomeId = blockTag.getString("biome");

					list.add(new ContraptionBlockRegistry.StoredBlock(x, y, z, state, biomeId));
				}

				if (!list.isEmpty()) {
					chunkMap.put(key, list);
				}
			}

			if (!chunkMap.isEmpty()) {
				data.put(id, chunkMap);
			}
		}

		return new PersistedData(data, windmills);
	}

	private static List<WindmillLODEntry> decodeWindmills(@Nullable
	CompoundTag root) {
		var entries = new ArrayList<WindmillLODEntry>();

		if (root == null || !root.contains("windmills", Tag.TAG_LIST)) {
			return entries;
		}

		for (Tag entryRaw : root.getList("windmills", Tag.TAG_COMPOUND)) {
			if (!(entryRaw instanceof CompoundTag entryTag)) {
				continue;
			}

			var identifierValue = resolveIdentifier(entryTag);
			var dimensionIdentifier = entryTag.getString("dimension");

			if (identifierValue.isBlank() || dimensionIdentifier.isBlank()) {
				continue;
			}

			if (!entryTag.contains("anchor", Tag.TAG_COMPOUND)) {
				continue;
			}

			var anchorPosition = NbtUtils.readBlockPos(entryTag.getCompound("anchor"));
			var axis = resolveAxis(entryTag.getString("axis"));
			var rotationSpeed = entryTag.getFloat("speed");
			var rotationAngle = entryTag.getFloat("angle");
			var lastSynchronizationTick = resolveSynchronizationTick(entryTag);

			try {
				var contraptionIdentifier = UUID.fromString(identifierValue);
				var entry = new WindmillLODEntry(contraptionIdentifier, dimensionIdentifier, anchorPosition, axis, rotationSpeed,
						rotationAngle, lastSynchronizationTick);
				entries.add(entry);
			} catch (IllegalArgumentException ignored) {
				// Skip invalid entry identifiers.
			}
		}

		return entries;
	}

	private static String resolveIdentifier(CompoundTag entryTag) {
		var identifierValue = entryTag.getString("identifier");

		if (!identifierValue.isBlank()) {
			return identifierValue;
		}

		return entryTag.getString("id");
	}

	private static long resolveSynchronizationTick(CompoundTag entryTag) {
		if (entryTag.contains("lastSynchronizationTick", Tag.TAG_LONG)) {
			return entryTag.getLong("lastSynchronizationTick");
		}

		return entryTag.getLong("lastSyncTick");
	}

	private static Direction.Axis resolveAxis(String axisName) {
		var axis = Direction.Axis.byName(axisName);

		if (axis == null) {
			return Direction.Axis.Y;
		}

		return axis;
	}

	private static int countEntries(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data) {
		var count = 0;

		for (var chunkMap : data.values()) {
			for (var blocks : chunkMap.values()) {
				count += blocks.size();
			}
		}

		return count;
	}

	private record PersistedData(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> chunkData,
			List<WindmillLODEntry> windmills) {
	}
}
