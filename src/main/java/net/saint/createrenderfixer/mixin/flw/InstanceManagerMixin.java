package net.saint.createrenderfixer.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.ratelimit.DistanceUpdateLimiter;

import net.saint.createrenderfixer.Mod;
import net.saint.createrenderfixer.utils.FreezeConditionUtil;

/**
 * Freezes dynamic instances beyond a configurable distance to avoid large batched buffer updates.
 */
@Mixin(value = InstanceManager.class, remap = false)
public abstract class InstanceManagerMixin {

	@Shadow
	protected DistanceUpdateLimiter frame;

	@Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true)
	private void crf$updateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY, int cZ,
			CallbackInfo callbackInfo) {
		if (!shouldAllowUpdateInstance(dynamicInstance, lookX, lookY, lookZ, cX, cY, cZ)) {
			callbackInfo.cancel();
			return;
		}

		if (!dynamicInstance.decreaseFramerateWithDistance()) {
			dynamicInstance.beginFrame();
			callbackInfo.cancel();
			return;
		}

		var worldPos = dynamicInstance.getWorldPosition();
		var dX = worldPos.getX() - cX;
		var dY = worldPos.getY() - cY;
		var dZ = worldPos.getZ() - cZ;

		if (frame.shouldUpdate(dX, dY, dZ)) {
			dynamicInstance.beginFrame();
		}

		callbackInfo.cancel();
	}

	private boolean shouldAllowUpdateInstance(DynamicInstance dynamicInstance, float lookX, float lookY, float lookZ, int cX, int cY,
			int cZ) {
		if (Mod.isInstanceBlacklisted(dynamicInstance)) {
			return true;
		}

		var position = dynamicInstance.getWorldPosition();

		if (FreezeConditionUtil.shouldFreezeAtPosition(position, cX, cY, cZ)) {
			return false;
		}

		return true;
	}
}
