package ca.fxco.moreculling;

import ca.fxco.moreculling.utils.CacheUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = MoreCulling.MOD_ID, dist = Dist.CLIENT)
public class MoreCullingNeoforge {
    public MoreCullingNeoforge(ModContainer container, IEventBus bus) {
        // Fleet build: no Cloth Config. Options are edited on the More Culling page of Sodium's video settings
        // (MoreCullingSodiumConfigBuilder) and stored in config/moreculling.toml.
        MoreCulling.init(FMLPaths.CONFIGDIR.get());

        bus.addListener(this::registerReloadListener);
        bus.addListener(this::onClientSetup);
    }

    public void registerReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(MoreCulling.RELOAD_LISTENER_ID, (barrier,
                                                           resourceManager,
                                                           executor, gameExecutor) ->
                executor.wait(Unit.INSTANCE).thenRunAsync(() -> {
                    ProfilerFiller profilerfiller = Profiler.get();
                    profilerfiller.push("listener");

                    CacheUtils.resetAllCache();

                    profilerfiller.pop();
                }, gameExecutor));
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }
}
