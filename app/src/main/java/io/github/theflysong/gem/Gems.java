package io.github.theflysong.gem;

import java.util.function.Supplier;

import io.github.theflysong.data.Identifier;
import io.github.theflysong.util.registry.Deferred;
import io.github.theflysong.util.registry.Registry;
import io.github.theflysong.util.registry.SimpleRegistry;

/**
 * 宝石系统
 *
 * @author theflysong
 * @date 2026年4月16日
 */
public class Gems {
    public static final Registry<Gem> GEMS = new SimpleRegistry<>();
    
    public static final Deferred<Gem> DIAMOND = register(
            "diamond",
            Gem::new);

    public static final Deferred<Gem> SAPPHIRE = register(
            "sapphire",
            Gem::new);

    public static final Deferred<Gem> JADE = register(
            "jade",
            Gem::new);

    public static final Deferred<Gem> CHIPPED = register(
            "chipped",
            GemChipped::new);
    
    public static final Deferred<Gem> FLAWED = register(
            "flawed",
            GemFlawed::new);
    
    public static final Deferred<Gem> FLAWLESS = register(
            "flawless",
            GemFlawless::new);
    
    public static final Deferred<Gem> EXQUISITE = register(
            "exquisite",
            GemExquisite::new);

    private Gems() {
    }

    public static Deferred<Gem> register(Identifier gemId, Supplier<Gem> supplier) {
        return GEMS.register(gemId, supplier);
    }

    public static Deferred<Gem> register(String gemId, Supplier<Gem> supplier) {
        return register(new Identifier(gemId), supplier);
    }

    public static void initialize() {
        GEMS.onInitialization();
    }
}
