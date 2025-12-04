package net.saint.createrenderfixer.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.Catwalks;

import net.minecraft.client.render.RenderLayer;

@Mixin(Catwalks.class)
public class CatwalksMixin {

	@ModifyArg(method = "buildStair", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<net.minecraft.client.render.RenderLayer>> redirectBuildStairLayer(Supplier<?> original) {
		return () -> () -> RenderLayer.getCutout();
	}

	@ModifyArg(method = "buildRailing", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<net.minecraft.client.render.RenderLayer>> redirectBuildRailingLayer(Supplier<?> original) {
		return () -> () -> RenderLayer.getCutout();
	}
}
