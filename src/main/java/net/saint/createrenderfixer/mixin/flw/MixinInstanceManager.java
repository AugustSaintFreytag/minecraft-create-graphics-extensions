package net.saint.createrenderfixer.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.saint.createrenderfixer.ModConfig;
import net.saint.createrenderfixer.mixin.BlockEntityInstanceAccessor;

/**
 * Freezes dynamic instances beyond a configurable distance to avoid large batched buffer updates.
 */
@Mixin(value = InstanceManager.class, remap = false)
public abstract class MixinInstanceManager {

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void crf$freezeFar(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
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

		BlockPos position = dynamicInstance.getWorldPosition();

		var dx = position.getX() - cX;
		var dy = position.getY() - cY;
		var dz = position.getZ() - cZ;

		var limit = ModConfig.freezeBlockDistance();

		if ((long) dx * dx + (long) dy * dy + (long) dz * dz > (long) limit * (long) limit) {
			callbackInfo.cancel();
		}
	}
}
