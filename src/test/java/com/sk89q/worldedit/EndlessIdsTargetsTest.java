package com.sk89q.worldedit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Test;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.forge.ForgeWorld;
import com.sk89q.worldedit.world.biome.BaseBiome;

/**
 * EndlessIDs overwrites these with mixins and fails to load if they change, so this fails the build
 * first.
 */
public class EndlessIdsTargetsTest {

    private static void assertMethod(Class<?> owner, String name, Class<?> returns, int modifiers,
        Class<?>... parameters) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        assertEquals(name, returns, method.getReturnType());
        assertTrue(name + " lost a modifier", (method.getModifiers() & modifiers) == modifiers);
    }

    @Test
    public void baseBlockMethodsAndFieldAreUnchanged() throws Exception {
        assertMethod(BaseBlock.class, "getId", int.class, Modifier.PUBLIC);
        assertMethod(BaseBlock.class, "getData", int.class, Modifier.PUBLIC);
        assertMethod(BaseBlock.class, "internalSetId", void.class, Modifier.PROTECTED | Modifier.FINAL, int.class);
        assertMethod(BaseBlock.class, "internalSetData", void.class, Modifier.PROTECTED | Modifier.FINAL, int.class);

        Field data = BaseBlock.class.getDeclaredField("data");
        assertEquals(short.class, data.getType());
        assertTrue(Modifier.isPrivate(data.getModifiers()));
    }

    @Test
    public void forgeWorldSetBiomeIsUnchanged() throws Exception {
        assertMethod(ForgeWorld.class, "setBiome", boolean.class, Modifier.PUBLIC, Vector2D.class, BaseBiome.class);
        assertMethod(ForgeWorld.class, "getWorld", net.minecraft.world.World.class, Modifier.PUBLIC);
    }
}
