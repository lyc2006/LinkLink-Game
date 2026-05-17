package io.github.theflysong.client.audio;

import org.jspecify.annotations.Nullable;

import io.github.theflysong.data.ResourceLocation;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.AL_LOOPING;
import static org.lwjgl.openal.AL10.AL_PAUSED;
import static org.lwjgl.openal.AL10.AL_PLAYING;
import static org.lwjgl.openal.AL10.AL_SOURCE_STATE;
import static org.lwjgl.openal.AL10.AL_STOPPED;
import static org.lwjgl.openal.AL10.AL_TRUE;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetSourcei;
import static org.lwjgl.openal.AL10.alSourcePause;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;

final class AudioTrack {
    enum Kind {
        MUSIC,
        SFX
    }

    private final int sourceId;
    private final Kind kind;
    private @Nullable ResourceLocation current;
    private boolean active;
    private boolean paused;

    AudioTrack(Kind kind) {
        this.kind = kind;
        this.sourceId = alGenSources();
    }

    int sourceId() {
        return sourceId;
    }

    Kind kind() {
        return kind;
    }

    @Nullable ResourceLocation current() {
        return current;
    }

    boolean isActive() {
        return active;
    }

    void play(int bufferId, ResourceLocation location, boolean loop, float gain) {
        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcei(sourceId, AL_LOOPING, loop ? AL_TRUE : 0);
        alSourcef(sourceId, AL_GAIN, gain);
        alSourcePlay(sourceId);
        current = location;
        active = true;
        paused = false;
    }

    void stop() {
        if (!active) {
            return;
        }
        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, 0);
        current = null;
        active = false;
        paused = false;
    }

    void pause() {
        if (!active || paused) {
            return;
        }
        alSourcePause(sourceId);
        paused = true;
    }

    void resume() {
        if (!active || !paused) {
            return;
        }
        alSourcePlay(sourceId);
        paused = false;
    }

    void applyGain(float gain) {
        alSourcef(sourceId, AL_GAIN, gain);
    }

    @Nullable ResourceLocation update() {
        if (!active || paused) {
            return null;
        }
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            ResourceLocation finished = current;
            alSourcei(sourceId, AL_BUFFER, 0);
            current = null;
            active = false;
            paused = false;
            return finished;
        }
        if (state == AL_PAUSED) {
            paused = true;
        }
        if (state == AL_PLAYING) {
            paused = false;
        }
        return null;
    }

    void close() {
        alSourceStop(sourceId);
        alSourcei(sourceId, AL_BUFFER, 0);
        alDeleteSources(sourceId);
        current = null;
        active = false;
        paused = false;
    }
}
