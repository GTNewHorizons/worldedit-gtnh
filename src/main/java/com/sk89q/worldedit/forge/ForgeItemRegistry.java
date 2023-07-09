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

package com.sk89q.worldedit.forge;

import javax.annotation.Nullable;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.world.registry.ItemRegistry;

public class ForgeItemRegistry implements ItemRegistry {

    @Nullable
    @Override
    public BaseItem createFromId(String id) {
        Item item = (Item) Item.itemRegistry.getObject(id);
        if (item != null) {
            return new BaseItem(Item.getIdFromItem(item), (short) item.getDamage(new ItemStack(item, 1)));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public BaseItem createFromId(int id) {
        Item item = (Item) Item.itemRegistry.getObjectById(id);
        if (item != null) {
            return new BaseItem(Item.getIdFromItem(item), (short) item.getDamage(new ItemStack(item, 1)));
        } else {
            return null;
        }
    }

}
