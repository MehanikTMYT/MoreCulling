package ca.fxco.moreculling;

import ca.fxco.moreculling.config.ConfigUpdater;
import ca.fxco.moreculling.config.MoreCullingConfig;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoreCulling {

    public static int CURRENT_VERSION = 1;

    public static ModelManager bakedModelManager = null;

    public static final String MOD_ID = "moreculling";
    public static final TagKey<Block> DONT_CULL = TagKey.create(BuiltInRegistries.BLOCK.key(),
            Identifier.fromNamespaceAndPath(MOD_ID, "dont_cull"));
    public static final Identifier RELOAD_LISTENER_ID = Identifier
            .fromNamespaceAndPath(MoreCulling.MOD_ID, "init_blockstate_culling_cache");

    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static MoreCullingConfig CONFIG;

    private static java.nio.file.Path configFile;

    /** Fleet build: config is read by MoreCullingConfigIO (same moreculling.toml), no Cloth Config / AutoConfig. */
    public static void init(java.nio.file.Path configDir) {
        configFile = ca.fxco.moreculling.config.MoreCullingConfigIO.file(configDir);
        CONFIG = ca.fxco.moreculling.config.MoreCullingConfigIO.load(configFile);
        ConfigUpdater.updateConfig(CONFIG);
        CONFIG.modCompatibility.defaultReturnValue(CONFIG.useOnModdedBlocksByDefault);
    }

    public static void saveConfig() {
        ca.fxco.moreculling.config.MoreCullingConfigIO.save(configFile, CONFIG);
    }
}
