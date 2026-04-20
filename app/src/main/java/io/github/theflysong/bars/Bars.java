package io.github.theflysong.bars;

import java.util.function.Supplier;

import io.github.theflysong.data.Identifier;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.util.event.EventPriority;
import io.github.theflysong.util.event.EventSubscriber;
import io.github.theflysong.util.event.SubscribeEvent;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

/**
 * 
 *
 * @author theflysong
 * @date 2026年4月21日
 */
public class Bars {
    public static final Registry<EnergyBar> BARS = new SimpleRegistry<>();

    public static final Deferred<EnergyBar> TOTAL = register(
            "total", TotalBar::new);

    private Bars() {
    }

    public static Deferred<EnergyBar> register(Identifier barId, Supplier<EnergyBar> supplier) {
        return BARS.register(barId, supplier);
    }

    public static Deferred<EnergyBar> register(String barId, Supplier<EnergyBar> supplier) {
        return register(new Identifier(barId), supplier);
    }

    @EventSubscriber
    public static final class InitializationListener {
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void onClientRegistriesInit(InitializationEvent event) {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("bars", BARS::onInitialization);
        }
    }
}
