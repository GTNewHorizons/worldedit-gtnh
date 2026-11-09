/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.command;

import static com.sk89q.worldedit.command.brigadier.CommandRunner.run;
import static com.sk89q.worldedit.command.brigadier.Nodes.integer;
import static com.sk89q.worldedit.command.brigadier.Nodes.literal;
import static com.sk89q.worldedit.command.brigadier.Nodes.word;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.command.ICommandSender;

import com.mojang.brigadier.CommandDispatcher;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.command.brigadier.LogMode;
import com.sk89q.worldedit.command.brigadier.Nodes;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.FlatRegionFunction;
import com.sk89q.worldedit.function.FlatRegionMaskingFilter;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.biome.BiomeData;
import com.sk89q.worldedit.world.registry.BiomeRegistry;

/**
 * Biome commands.
 *
 * <ul>
 * <li>{@code //biomelist [page]} (alias {@code //biomels}): list all biomes</li>
 * <li>{@code //biomeinfo [-p] [-t]}: show the biome of the selection, your position or the block you look at</li>
 * <li>{@code //setbiome [-p] <biome>}: change the biome of the selection or your position</li>
 * </ul>
 */
public final class BiomeCommands {

    private static final int BIOMES_PER_PAGE = 19;

    private BiomeCommands() {}

    // spotless:off
    public static void register(CommandDispatcher<ICommandSender> dispatcher) {
        Nodes.register(dispatcher, literal("biomelist")
                .executes(run("worldedit.biome.list", c -> biomeList(c.player(), 1)))
                .then(integer("page", 1)
                        .executes(run("worldedit.biome.list", c -> biomeList(c.player(), c.integer("page"))))),
                "biomels");

        Nodes.registerWithFlags(dispatcher, literal("biomeinfo")
                .executes(run("worldedit.biome.info", c -> biomeInfo(c.player(), c.session(), c.flag('t'), c.flag('p')))),
                "pt");

        Nodes.registerWithFlags(dispatcher, literal("/setbiome").executes(Nodes.USAGE)
                .then(word("biome")
                        .executes(run("worldedit.biome.set", LogMode.REGION, c ->
                                setBiome(c.player(), c.session(), c.editSession(), c.biome("biome"), c.flag('p'))))),
                "p");
    }
    // spotless:on

    private static void biomeList(Player player, int page) {
        int offset = page < 2 ? 0 : (page - 1) * BIOMES_PER_PAGE;
        int count = 0;
        page = Math.max(1, page);

        BiomeRegistry biomeRegistry = player.getWorld()
            .getWorldData()
            .getBiomeRegistry();
        List<BaseBiome> biomes = biomeRegistry.getBiomes();
        int totalPages = biomes.size() / BIOMES_PER_PAGE + 1;
        player.print("Available Biomes (page " + page + "/" + totalPages + ") :");
        for (BaseBiome biome : biomes) {
            if (offset > 0) {
                offset--;
            } else {
                BiomeData data = biomeRegistry.getData(biome);
                if (data != null) {
                    player.print(" " + data.getName());
                    if (++count == BIOMES_PER_PAGE) {
                        break;
                    }
                } else {
                    player.print(" <unknown #" + biome.getId() + ">");
                }
            }
        }
    }

    private static void biomeInfo(Player player, LocalSession session, boolean lookingAt, boolean atPosition)
        throws Exception {
        BiomeRegistry biomeRegistry = player.getWorld()
            .getWorldData()
            .getBiomeRegistry();
        Set<BaseBiome> biomes = new HashSet<BaseBiome>();
        String qualifier;

        if (lookingAt) {
            Vector blockPosition = player.getBlockTrace(300);
            if (blockPosition == null) {
                player.printError("No block in sight!");
                return;
            }

            biomes.add(
                player.getWorld()
                    .getBiome(blockPosition.toVector2D()));
            qualifier = "at line of sight point";
        } else if (atPosition) {
            biomes.add(
                player.getWorld()
                    .getBiome(
                        player.getPosition()
                            .toVector2D()));
            qualifier = "at your position";
        } else {
            World world = player.getWorld();
            Region region = session.getSelection(world);

            if (region instanceof FlatRegion) {
                for (Vector2D pt : ((FlatRegion) region).asFlatRegion()) {
                    biomes.add(world.getBiome(pt));
                }
            } else {
                for (Vector pt : region) {
                    biomes.add(world.getBiome(pt.toVector2D()));
                }
            }

            qualifier = "in your selection";
        }

        player.print(biomes.size() != 1 ? "Biomes " + qualifier + ":" : "Biome " + qualifier + ":");
        for (BaseBiome biome : biomes) {
            BiomeData data = biomeRegistry.getData(biome);
            if (data != null) {
                player.print(" " + data.getName());
            } else {
                player.print(" <unknown #" + biome.getId() + ">");
            }
        }
    }

    private static void setBiome(Player player, LocalSession session, EditSession editSession, BaseBiome target,
        boolean atPosition) throws Exception {
        World world = player.getWorld();
        Region region;
        Mask mask = editSession.getMask();
        Mask2D mask2d = mask != null ? mask.toMask2D() : null;

        if (atPosition) {
            region = new CuboidRegion(player.getPosition(), player.getPosition());
        } else {
            region = session.getSelection(world);
        }

        FlatRegionFunction replace = new BiomeReplace(editSession, target);
        if (mask2d != null) {
            replace = new FlatRegionMaskingFilter(mask2d, replace);
        }
        FlatRegionVisitor visitor = new FlatRegionVisitor(Regions.asFlatRegion(region), replace);
        Operations.completeLegacy(visitor);

        player.print(
            "Biomes were changed in " + visitor.getAffected()
                + " columns. You may have to rejoin your game (or close and reopen your world) to see a change.");
    }
}
