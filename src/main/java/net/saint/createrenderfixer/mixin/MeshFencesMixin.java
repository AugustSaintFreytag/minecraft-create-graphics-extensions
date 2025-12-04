package net.saint.createrenderfixer.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.github.talrey.createdeco.api.MeshFences;

import net.minecraft.client.render.RenderLayer;

@Mixin(MeshFences.class)
public class MeshFencesMixin {

	@ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/builders/BlockBuilder;addLayer(Ljava/util/function/Supplier;)Lcom/tterrag/registrate/builders/BlockBuilder;"), index = 0, remap = false)
	private static Supplier<Supplier<net.minecraft.client.render.RenderLayer>> redirectMeshFenceLayer(Supplier<?> original) {
		return () -> () -> RenderLayer.getCutout();
	}
}
