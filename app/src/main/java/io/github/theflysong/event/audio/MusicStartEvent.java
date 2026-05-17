package io.github.theflysong.event.audio;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.util.event.Event;

public final class MusicStartEvent extends Event {
    private final ResourceLocation location;

    public MusicStartEvent(ResourceLocation location) {
        this.location = location;
    }

    public ResourceLocation location() {
        return location;
    }
}
