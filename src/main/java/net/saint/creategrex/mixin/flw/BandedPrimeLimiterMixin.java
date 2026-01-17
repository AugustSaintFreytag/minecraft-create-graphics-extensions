package net.saint.creategrex.mixin.flw;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jozufozu.flywheel.backend.instancing.ratelimit.BandedPrimeLimiter;

import net.saint.creategrex.Mod;

@Mixin(BandedPrimeLimiter.class)
public abstract class BandedPrimeLimiterMixin {

	@Inject(method = "shouldUpdate", at = @At("HEAD"), cancellable = true, remap = false)
	private void cge$shouldUpdate(int dX, int dY, int dZ, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (Mod.CONFIG.forceDisableRateLimiting) {
			callbackInfo.setReturnValue(true);
		}
	}

}
