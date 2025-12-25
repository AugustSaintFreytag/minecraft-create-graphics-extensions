package net.saint.createrenderfixer.dh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

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
			CompoundTag root = NbtIo.readCompressed(savePath.toFile());
			Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data = decode(
					server.registryAccess().lookupOrThrow(Registries.BLOCK), root);
			ContraptionBlockRegistry.loadPersistent(data);
			Mod.LOGGER.info("Loaded {} contraption chunks for DH overrides from file.", countEntries(data));
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
			var tag = encode(snapshot);

			NbtIo.writeCompressed(tag, savePath.toFile());
		} catch (Exception exception) {
			Mod.LOGGER.error("Could not save contraption persistence data to file.", exception);
		}
	}

	private static Path getSavePath(MinecraftServer server) {
		return server.getServerDirectory().toPath().resolve("create-render-fixer").resolve(FILE_NAME);
	}

	// Encoding

	private static CompoundTag encode(Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data) {
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
		return root;
	}

	// Decoding

	private static Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> decode(HolderGetter<Block> blocks, @Nullable
	CompoundTag root) throws IOException {
		Map<String, Map<Long, List<ContraptionBlockRegistry.StoredBlock>>> data = new HashMap<>();
		if (root == null || !root.contains("dims", Tag.TAG_LIST)) {
			return data;
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

		return data;
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
}
