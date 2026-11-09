package com.sk89q.worldedit.forge.command;

import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.sk89q.worldedit.command.brigadier.CommandCall;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import com.sk89q.worldedit.forge.compat.ModRotationBlockTransformHook;
import com.sk89q.worldedit.forge.compat.rotation.RotationMappings;

/**
 * {@code //we reloadmappings}: reload the rotation mappings of modded blocks from disk.
 */
public final class ForgeRotationCommands {

    private ForgeRotationCommands() {}

    /**
     * The command, to be registered under {@code //worldedit}.
     */
    public static LiteralCommandNode<ICommandSender> node() {
        return literal("reloadmappings", "Reload rotation mappings")
            .executes(run("worldedit.reload", ForgeRotationCommands::reloadMappings))
            .build();
    }

    private static void reloadMappings(CommandCall call) {
        RotationMappings.init(ForgeWorldEdit.inst.getWorkingDir());
        ModRotationBlockTransformHook hook = ForgeWorldEdit.inst.getModRotationHook();
        if (hook != null) {
            hook.clearCache();
        }
        call.actor()
            .print("Rotation mappings reloaded!");
    }
}
