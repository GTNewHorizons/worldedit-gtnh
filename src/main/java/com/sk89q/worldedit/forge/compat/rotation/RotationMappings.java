package com.sk89q.worldedit.forge.compat.rotation;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTrapDoor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sk89q.worldedit.forge.compat.rotation.types.FourRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.PillarRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.RotationBase;
import com.sk89q.worldedit.forge.compat.rotation.types.StairRotation;
import com.sk89q.worldedit.forge.compat.rotation.types.TrapdoorRotation;
import com.sk89q.worldedit.util.gson.GsonUtil;

import cpw.mods.fml.common.registry.GameData;

/**
 * Manages loading and saving rotation mappings.
 */
public class RotationMappings {

    private static RotationMappings instance;

    private final File dir;
    private final Gson gson = GsonUtil.createBuilder()
        .setPrettyPrinting()
        .create();
    private final Map<String, RotationMapping> mappings = new HashMap<>();

    private RotationMappings(File dir) {
        this.dir = dir;
        loadAll();
    }

    public static void init(File workingDir) {
        File mapDir = new File(workingDir, "mappings");
        if (!mapDir.exists()) {
            mapDir.mkdirs();
        }
        instance = new RotationMappings(mapDir);
    }

    public static RotationMappings getInstance() {
        return instance;
    }

    public RotationMapping get(String name) {
        return mappings.get(name);
    }

    private void loadAll() {
        for (RotationType type : RotationType.values()) {
            File file = new File(
                dir,
                type.name()
                    .toLowerCase() + ".json");
            loadFile(file, type);
        }
        if (mappings.isEmpty()) {
            generateDefaults();
            saveAll();
        }
    }

    private void loadFile(File file, RotationType type) {
        if (!file.exists()) return;
        try (FileReader r = new FileReader(file)) {
            JsonObject root = gson.fromJson(r, JsonObject.class);
            if (root == null) return;
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                JsonObject obj = e.getValue()
                    .getAsJsonObject();
                RotationMapping rm = parseMapping(type, obj);
                if (rm != null) {
                    mappings.put(e.getKey(), rm);
                }
            }
        } catch (IOException ignore) {}
    }

    private RotationMapping parseMapping(RotationType fileType, JsonObject obj) {
        String as = fileType == RotationType.OTHER && obj.has("type") ? obj.get("type")
            .getAsString()
            .toUpperCase() : fileType.name();

        switch (as) {
            case "STAIR":
            case "STAIRS":
                int[] top = arr(obj.getAsJsonArray("top"));
                int[] bottom = arr(obj.getAsJsonArray("bottom"));
                return new RotationMapping(fileType, new StairRotation() {

                    {
                        setTop(top);
                        setBottom(bottom);
                    }
                });
            case "PILLAR":
                PillarRotation p = new PillarRotation();
                if (obj.has("groups")) {
                    JsonObject gs = obj.getAsJsonObject("groups");
                    int[][] arr = new int[gs.entrySet()
                        .size()][3];
                    int idx = 0;
                    for (Map.Entry<String, JsonElement> e : gs.entrySet()) {
                        arr[idx++] = arr(
                            e.getValue()
                                .getAsJsonArray());
                    }
                    p.setGroups(arr);
                } else {
                    p.setX(
                        obj.get("x")
                            .getAsInt());
                    p.setY(
                        obj.get("y")
                            .getAsInt());
                    p.setZ(
                        obj.get("z")
                            .getAsInt());
                }
                return new RotationMapping(fileType, p);
            case "FOUR":
            case "FENCE_GATE":
                int[] m = arr(obj.getAsJsonArray("metas"));
                return new RotationMapping(fileType, new FourRotation() {

                    {
                        setMetas(m);
                    }
                });
            case "TRAP_DOOR":
                TrapdoorRotation tr = new TrapdoorRotation();
                if (obj.has("bottom")) {
                    JsonObject b = obj.getAsJsonObject("bottom");
                    if (b.has("closed")) tr.setBottomClosed(arr(b.getAsJsonArray("closed")));
                    if (b.has("open")) tr.setBottomOpen(arr(b.getAsJsonArray("open")));
                }
                if (obj.has("top")) {
                    JsonObject t = obj.getAsJsonObject("top");
                    if (t.has("closed")) tr.setTopClosed(arr(t.getAsJsonArray("closed")));
                    if (t.has("open")) tr.setTopOpen(arr(t.getAsJsonArray("open")));
                }
                return new RotationMapping(fileType, tr);
            default:
                return null;
        }
    }

    private int[] arr(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < a.size(); i++) out[i] = a.get(i)
            .getAsInt();
        return out;
    }

    private JsonArray toArray(int[] a) {
        JsonArray j = new JsonArray();
        for (int v : a) j.add(new com.google.gson.JsonPrimitive(v));
        return j;
    }

    private void saveAll() {
        Map<RotationType, JsonObject> byType = new HashMap<>();
        for (RotationType t : RotationType.values()) {
            byType.put(t, new JsonObject());
        }
        for (Map.Entry<String, RotationMapping> e : mappings.entrySet()) {
            JsonObject obj = new JsonObject();
            RotationMapping rm = e.getValue();
            RotationBase base = rm.getBase();
            if (base instanceof StairRotation s) {
                obj.add("top", toArray(s.getTop()));
                obj.add("bottom", toArray(s.getBottom()));
                if (rm.getType() == RotationType.OTHER) {
                    obj.addProperty("type", "STAIR");
                }
            } else if (base instanceof PillarRotation p) {
                JsonObject groups = new JsonObject();
                int idx = 0;
                for (int[] g : p.getGroups()) {
                    groups.add(String.valueOf(idx++), toArray(g));
                }
                obj.add("groups", groups);
                if (rm.getType() == RotationType.OTHER) {
                    obj.addProperty("type", "PILLAR");
                }
            } else if (base instanceof FourRotation f) {
                obj.add("metas", toArray(f.getMetas()));
                if (rm.getType() == RotationType.OTHER) {
                    obj.addProperty("type", "FOUR");
                }
            } else if (base instanceof TrapdoorRotation t) {
                JsonObject top = new JsonObject();
                top.add("closed", toArray(t.getTopClosed()));
                top.add("open", toArray(t.getTopOpen()));
                JsonObject bottom = new JsonObject();
                bottom.add("closed", toArray(t.getBottomClosed()));
                bottom.add("open", toArray(t.getBottomOpen()));
                obj.add("top", top);
                obj.add("bottom", bottom);
                if (rm.getType() == RotationType.OTHER) {
                    obj.addProperty("type", "TRAP_DOOR");
                }
            }
            byType.get(rm.getType())
                .add(e.getKey(), obj);
        }
        for (RotationType t : RotationType.values()) {
            File file = new File(
                dir,
                t.name()
                    .toLowerCase() + ".json");
            try (FileWriter w = new FileWriter(file)) {
                String header = "// Generated by WorldEdit - rotation mappings for " + t.name()
                    .toLowerCase();
                if (t == RotationType.OTHER) {
                    header += "\n"
                        + "// type values: STAIR (top/bottom arrays), PILLAR (groups), FOUR (metas array), TRAP_DOOR"
                        + "\n"
                        + "// Example: \"mod:block\": { \"type\": \"FOUR\", \"metas\": [0,1,2,3] }";
                }
                w.write(header + "\n");
                gson.toJson(byType.get(t), w);
            } catch (IOException ignore) {}
        }
    }

    private void generateDefaults() {
        Iterator<?> it = GameData.getBlockRegistry()
            .iterator();
        while (it.hasNext()) {
            Block block = (Block) it.next();
            Object identifier = GameData.getBlockRegistry()
                .getNameForObject(block);
            String name = identifier == null ? null : identifier.toString();
            if (name == null || name.startsWith("minecraft:")) continue;
            RotationMapping mapping = null;
            if (block instanceof BlockStairs) {
                StairRotation sr = RotationUtils.defaultStairs();
                mapping = new RotationMapping(RotationType.STAIRS, sr);
            } else if (block instanceof BlockRotatedPillar) {
                PillarRotation pr = RotationUtils.defaultPillar();
                mapping = new RotationMapping(RotationType.PILLAR, pr);
            } else if (block instanceof BlockFenceGate || block instanceof BlockButton) {
                FourRotation fr = RotationUtils.defaultFour(block instanceof BlockButton);
                mapping = new RotationMapping(
                    block instanceof BlockFenceGate ? RotationType.FENCE_GATE : RotationType.OTHER,
                    fr);
            } else if (block instanceof BlockTrapDoor) {
                TrapdoorRotation tr = RotationUtils.defaultTrapdoor();
                mapping = new RotationMapping(RotationType.TRAP_DOOR, tr);
            }
            if (mapping != null) {
                mappings.put(name, mapping);
            }
        }
    }

    /**
     * Add or replace a mapping entry.
     */
    public void put(String name, RotationMapping mapping) {
        mappings.put(name, mapping);
    }

    /**
     * Persist current mappings to disk.
     */
    public void save() {
        saveAll();
    }
}
