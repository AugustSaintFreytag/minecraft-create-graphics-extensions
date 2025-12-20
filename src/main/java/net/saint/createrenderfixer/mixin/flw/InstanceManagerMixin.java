package net.saint.createrenderfixer.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.saint.createrenderfixer.FreezeConditionUtil;
import net.saint.createrenderfixer.ModConfig;
import net.saint.createrenderfixer.mixin.BlockEntityInstanceAccessor;

/**
 * Freezes dynamic instances beyond a configurable distance to avoid large batched buffer updates.
 */
@Mixin(value = InstanceManager.class, remap = false)
public abstract class InstanceManagerMixin {

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void crf$updateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
			CallbackInfo callbackInfo) {
		if (dynamicInstance instanceof BlockEntityInstance<?> blockEntityInstance) {
			var blockEntity = ((BlockEntityInstanceAccessor<BlockEntity>) blockEntityInstance).getBlockEntity();

			if (blockEntity != null && ModConfig.isFreezeBlacklisted(blockEntity.getType())) {
				return;
			}
		}

		var position = dynamicInstance.getWorldPosition();

		if (FreezeConditionUtil.shouldFreezePosition(position, cX, cY, cZ)) {
			callbackInfo.cancel();
			return;
		}
	}
}
