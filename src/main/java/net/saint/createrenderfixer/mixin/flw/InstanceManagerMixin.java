package net.saint.createrenderfixer.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.saint.createrenderfixer.ModConfig;
import net.saint.createrenderfixer.mixin.BlockEntityInstanceAccessor;
import net.saint.createrenderfixer.mixin.LevelRendererAccessor;
import net.saint.createrenderfixer.mixin.RenderChunkInfoAccessor;

/**
 * Freezes dynamic instances beyond a configurable distance to avoid large batched buffer updates.
 */
@Mixin(value = InstanceManager.class, remap = false)
public abstract class InstanceManagerMixin {

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void crf$updateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
			CallbackInfo callbackInfo) {
		if (!ModConfig.freezeDistantInstances()) {
			return;
		}

		if (ModConfig.freezeBlockDistance() <= 0) {
			return;
		}

		if (dynamicInstance instanceof BlockEntityInstance<?> blockEntityInstance) {
			var blockEntity = ((BlockEntityInstanceAccessor<BlockEntity>) blockEntityInstance).getBlockEntity();

			if (blockEntity != null && ModConfig.isFreezeBlacklisted(blockEntity.getType())) {
				return;
			}
		}

		var position = dynamicInstance.getWorldPosition();

		if (ModConfig.freezeOccludedInstances() && crf$positionIsInOccludedSection(position)) {
			callbackInfo.cancel();
			return;
		}

		var dx = position.getX() - cX;
		var dy = position.getY() - cY;
		var dz = position.getZ() - cZ;

		var limit = ModConfig.freezeBlockDistance();

		if ((long) dx * dx + (long) dy * dy + (long) dz * dz > (long) limit * (long) limit) {
			callbackInfo.cancel();
		}
	}

	private boolean crf$positionIsInOccludedSection(BlockPos position) {
		var client = Minecraft.getInstance();
		var levelRenderer = client.levelRenderer;

		if (!(levelRenderer instanceof LevelRendererAccessor accessor)) {
			return false;
		}

		var chunksInFrustum = accessor.getRenderChunksInFrustum();

		if (position == null || chunksInFrustum == null || chunksInFrustum.isEmpty()) {
			return false;
		}

		var section = SectionPos.of(position);
		var sectionX = section.getX();
		var sectionY = section.getY();
		var sectionZ = section.getZ();

		for (var chunkInfo : chunksInFrustum) {
			var renderChunk = ((RenderChunkInfoAccessor) chunkInfo).getChunk();

			if (renderChunk == null) {
				continue;
			}

			var origin = renderChunk.getOrigin();
			if (origin == null) {
				continue;
			}

			var renderSection = SectionPos.of(origin);
			if (renderSection.getX() == sectionX && renderSection.getY() == sectionY && renderSection.getZ() == sectionZ) {
				return false;
			}
		}

		return true;
	}
}
