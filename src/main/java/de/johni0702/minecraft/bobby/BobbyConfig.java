package de.johni0702.minecraft.bobby;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class BobbyConfig {
    public static ForgeConfigSpec ConfigSpec;

    private static ConfigValue<Boolean> enabled;
    private static ConfigValue<Boolean> noBlockEntities;
    private static ConfigValue<Boolean> taintFakeChunks;
    private static ConfigValue<Integer> unloadDelaySecs;
    private static ConfigValue<Integer> deleteUnusedRegionsAfterDays;
    private static ConfigValue<Integer> maxRenderDistance;
    private static ConfigValue<Integer> viewDistanceOverwrite;

    static
    {
        var builder = new ConfigBuilder("Bobby Reforged Settings");

        builder.Block("General Settings", b -> {

            enabled = b.define("Enable Bobby", true);
            noBlockEntities = b.define("Do not load block entities in fake chunks", true);
            taintFakeChunks = b.define("Reduce the light levels in fake chunks", true);
            maxRenderDistance = b.define("Max Render Distance", 32);
            viewDistanceOverwrite = b.define("Integrated Server View Distance override", 0);
        });

        builder.Block("Unloading", b -> {

            unloadDelaySecs = b.define("Delay for unloading of chunks which are outside your view distance (seconds)", 60);
            deleteUnusedRegionsAfterDays = b.define("Delay for deleting regions from the disk cache (days)", -1);
        });

        ConfigSpec = builder.Save();
        BobbyConfig.loadConfig(FMLPaths.CONFIGDIR.get().resolve("bobby.toml"));
    }

    public static void loadConfig(Path path) {
        final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();

        configData.load();
        ConfigSpec.setConfig(configData);
    }

    public static boolean isNoBlockEntities() {
        return noBlockEntities.get();
    }

    public static boolean isEnabled() {
        return enabled.get();}

    public static boolean isTaintFakeChunks() {
        return taintFakeChunks.get();
    }

    public static int getUnloadDelaySecs() {
        return unloadDelaySecs.get();
    }

    public static int getDeleteUnusedRegionsAfterDays() {
        return deleteUnusedRegionsAfterDays.get();
    }

    public static int getMaxRenderDistance() {
        return maxRenderDistance.get();
    }

    public static int getViewDistanceOverwrite() {
        return viewDistanceOverwrite.get();
    }
}
