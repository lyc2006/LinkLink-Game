package io.github.theflysong.event.audio;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.util.event.Event;

public final class MusicEndEvent extends Event {
    private final ResourceLocation location;

    public MusicEndEvent(ResourceLocation location) {
        this.location = location;
    }

    public ResourceLocation location() {
        return location;
    }
}
