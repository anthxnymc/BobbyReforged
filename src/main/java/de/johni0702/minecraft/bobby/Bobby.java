package de.johni0702.minecraft.bobby;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import de.johni0702.minecraft.bobby.mixin.SimpleOptionAccessor;
import de.johni0702.minecraft.bobby.mixin.ValidatingIntSliderCallbacksAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Mod("bobby")
public class Bobby{
    static final Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "bobby";

    private static Bobby instance;
    public static Bobby getInstance() {
        return instance;
    }

    public Bobby() {
        instance = this;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::onInitializeClient);
    }

    public void onInitializeClient() {
        Bobby.MaxRenderDistanceConfigHandler.init();

        Util.getIoWorkerExecutor().submit(Bobby::cleanupOldWorlds);
    }


    public boolean isEnabled() {
        return BobbyConfig.isEnabled() && (MinecraftClient.getInstance().getServer() == null || BobbyConfig.getViewDistanceOverwrite() != 0);
    }


    static void cleanupOldWorlds() {
        int deleteUnusedRegionsAfterDays = BobbyConfig.getDeleteUnusedRegionsAfterDays();
        if (deleteUnusedRegionsAfterDays < 0) {
            return;
        }

        Path basePath = MinecraftClient.getInstance().runDirectory.toPath().resolve(".bobby");

        List<Path> toBeDeleted;
        try (Stream<Path> stream = Files.walk(basePath, 4)) {
            toBeDeleted = stream
                    .filter(it -> basePath.relativize(it).getNameCount() == 4)
                    .filter(it -> {
                        try {
                            return LastAccessFile.isEverythingOlderThan(it, deleteUnusedRegionsAfterDays);
                        } catch (IOException e) {
                            LOGGER.error("Failed to read last used file in " + it + ":", e);
                            return false;
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Failed to index bobby cache for cleanup:", e);
            return;
        }

        for (Path path : toBeDeleted) {
            try {
                //noinspection UnstableApiUsage
                MoreFiles.deleteRecursively(path, RecursiveDeleteOption.ALLOW_INSECURE);

                deleteParentsIfEmpty(path);
            } catch (IOException e) {
                LOGGER.error("Failed to delete " + path + ":", e);
            }
        }
    }

    private static void deleteParentsIfEmpty(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent == null) {
            return;
        }
        try (Stream<Path> stream = Files.list(parent)) {
            if (stream.findAny().isPresent()) {
                return;
            }
        }
        Files.delete(parent);
        deleteParentsIfEmpty(parent);
    }

    static class MaxRenderDistanceConfigHandler {
        private static int oldMaxRenderDistance = 0;

        public static void init()
        {
            MinecraftClient.getInstance().submit(() -> Bobby.MaxRenderDistanceConfigHandler.setMaxRenderDistance(BobbyConfig.getMaxRenderDistance(), true));
        }

        @SuppressWarnings({"ConstantConditions", "unchecked"})
        private static void setMaxRenderDistance(int newMaxRenderDistance, boolean increaseOnly) {
            if (oldMaxRenderDistance == newMaxRenderDistance) {
                return;
            }
            oldMaxRenderDistance = newMaxRenderDistance;

            SimpleOption<Integer> viewDistance = MinecraftClient.getInstance().options.getViewDistance();
            if (viewDistance.getCallbacks() instanceof SimpleOption.ValidatingIntSliderCallbacks callbacks) {
                ValidatingIntSliderCallbacksAccessor callbacksAcc = (ValidatingIntSliderCallbacksAccessor)(Object) callbacks;
                if (increaseOnly) {
                    callbacksAcc.setMaxInclusive(Math.max(callbacks.maxInclusive(), newMaxRenderDistance));
                } else {
                    callbacksAcc.setMaxInclusive(newMaxRenderDistance);
                }
                SimpleOptionAccessor<Integer> optionAccessor = (SimpleOptionAccessor<Integer>)(Object) viewDistance;
                optionAccessor.setCodec(callbacks.codec());
            }
        }
    }
}
