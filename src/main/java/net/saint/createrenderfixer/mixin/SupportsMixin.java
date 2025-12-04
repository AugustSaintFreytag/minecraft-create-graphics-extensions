package net.saint.createrenderfixer.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.Supports;

import net.minecraft.client.render.RenderLayer;

@Mixin(Supports.class)
public class SupportsMixin {

	@ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<net.minecraft.client.render.RenderLayer>> redirectAddLayer(
			Supplier<?> original) {
		return () -> () -> RenderLayer.getCutout();
	}

}
