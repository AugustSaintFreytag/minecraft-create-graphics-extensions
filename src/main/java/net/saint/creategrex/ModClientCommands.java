package net.saint.creategrex;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;

public final class ModClientCommands {

	// Init

	public static void init() {
		ClientCommandRegistrationCallback.EVENT.register(ModClientCommands::register);
	}

	// Registration

	private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext context) {
		dispatcher
				.register(literal("create-grexc").then(literal("resetWindmillRenderer").executes(ModClientCommands::resetWindmillRenderer))
						.then(literal("reloadWindmillMaterials").executes(ModClientCommands::reloadWindmillMaterials)));
	}

	// Commands

	private static int resetWindmillRenderer(CommandContext<FabricClientCommandSource> context) {
		ModClient.WINDMILL_LOD_RENDER_MANAGER.clear();
		return Command.SINGLE_SUCCESS;
	}

	private static int reloadWindmillMaterials(CommandContext<FabricClientCommandSource> context) {
		ModClient.WINDMILL_LOD_MATERIAL_MANAGER.reloadFromConfig();
		return Command.SINGLE_SUCCESS;
	}

}
