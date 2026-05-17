package io.github.theflysong.client.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

import io.github.theflysong.data.ResourceLocation;
import io.github.theflysong.data.ResourceLoader;
import io.github.theflysong.data.ResourceType;
import io.github.theflysong.data.audio.AudioData;
import io.github.theflysong.event.GameEvents;
import io.github.theflysong.event.InitializationEvent;
import io.github.theflysong.event.audio.MusicEndEvent;
import io.github.theflysong.event.audio.MusicStartEvent;
import io.github.theflysong.util.Side;
import io.github.theflysong.util.SideOnly;
import io.github.theflysong.util.event.EventPriority;
import io.github.theflysong.util.event.EventSubscriber;
import io.github.theflysong.util.event.SubscribeEvent;

import static io.github.theflysong.App.LOGGER;
import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;

@SideOnly(Side.CLIENT)
public final class AudioManager {
    public static final ResourceLocation TITLE_MUSIC = new ResourceLocation(ResourceType.MUSIC, "title.wav");
    public static final ResourceLocation BUTTON_UP = new ResourceLocation(ResourceType.SOUND, "button_up.ogg");

    private static final int DEFAULT_MUSIC_TRACKS = 1;
    private static final int DEFAULT_SFX_TRACKS = 2;

    private static final List<AudioTrack> MUSIC_TRACKS = new ArrayList<>();
    private static final List<AudioTrack> SFX_TRACKS = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> BUFFER_CACHE = new ConcurrentHashMap<>();

    private static long device;
    private static long context;
    private static boolean initialized;

    private static float masterVolume = 1.0f;
    private static float musicVolume = 0.8f;
    private static float sfxVolume = 1.0f;

    private AudioManager() {
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        device = alcOpenDevice((ByteBuffer) null);
        if (device == MemoryUtil.NULL) {
            throw new IllegalStateException("Failed to open OpenAL device");
        }

        context = alcCreateContext(device, (IntBuffer) null);
        if (context == MemoryUtil.NULL) {
            alcCloseDevice(device);
            device = MemoryUtil.NULL;
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        alcMakeContextCurrent(context);
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        AL.createCapabilities(alcCaps);

        for (int i = 0; i < DEFAULT_MUSIC_TRACKS; i++) {
            MUSIC_TRACKS.add(new AudioTrack(AudioTrack.Kind.MUSIC));
        }
        for (int i = 0; i < DEFAULT_SFX_TRACKS; i++) {
            SFX_TRACKS.add(new AudioTrack(AudioTrack.Kind.SFX));
        }

        initialized = true;
        LOGGER.info("OpenAL initialized: {} music track(s), {} sfx track(s)",
                MUSIC_TRACKS.size(), SFX_TRACKS.size());
    }

    public static void shutdown() {
        if (!initialized) {
            return;
        }

        stopAll(false);
        for (AudioTrack track : MUSIC_TRACKS) {
            track.close();
        }
        for (AudioTrack track : SFX_TRACKS) {
            track.close();
        }
        MUSIC_TRACKS.clear();
        SFX_TRACKS.clear();

        for (int bufferId : BUFFER_CACHE.values()) {
            alDeleteBuffers(bufferId);
        }
        BUFFER_CACHE.clear();

        alcMakeContextCurrent(MemoryUtil.NULL);
        if (context != MemoryUtil.NULL) {
            alcDestroyContext(context);
            context = MemoryUtil.NULL;
        }
        if (device != MemoryUtil.NULL) {
            alcCloseDevice(device);
            device = MemoryUtil.NULL;
        }
        initialized = false;
        LOGGER.info("OpenAL shutdown complete");
    }

    public static void update() {
        if (!initialized) {
            return;
        }

        for (AudioTrack track : MUSIC_TRACKS) {
            ResourceLocation finished = track.update();
            if (finished != null) {
                GameEvents.BUS.post(new MusicEndEvent(finished));
            }
        }
        for (AudioTrack track : SFX_TRACKS) {
            track.update();
        }
    }

    public static void playMusic(ResourceLocation location) {
        if (!initialized || location == null) {
            return;
        }

        stopMusic(true);
        AudioTrack track = ensureMusicTrack();
        int bufferId = bufferFor(location);
        track.play(bufferId, location, false, resolveGain(AudioTrack.Kind.MUSIC));
        GameEvents.BUS.post(new MusicStartEvent(location));
    }

    public static void stopMusic(boolean postEnd) {
        for (AudioTrack track : MUSIC_TRACKS) {
            if (!track.isActive()) {
                continue;
            }
            ResourceLocation current = track.current();
            track.stop();
            if (postEnd && current != null) {
                GameEvents.BUS.post(new MusicEndEvent(current));
            }
        }
    }

    public static void playSfx(ResourceLocation location) {
        if (!initialized || location == null) {
            return;
        }

        AudioTrack track = findAvailableSfxTrack();
        int bufferId = bufferFor(location);
        track.play(bufferId, location, false, resolveGain(AudioTrack.Kind.SFX));
    }

    public static void pauseAll() {
        for (AudioTrack track : MUSIC_TRACKS) {
            track.pause();
        }
        for (AudioTrack track : SFX_TRACKS) {
            track.pause();
        }
    }

    public static void resumeAll() {
        for (AudioTrack track : MUSIC_TRACKS) {
            track.resume();
        }
        for (AudioTrack track : SFX_TRACKS) {
            track.resume();
        }
    }

    public static void setMasterVolume(float volume) {
        masterVolume = clamp(volume);
        applyVolumes();
    }

    public static void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        applyVolumes();
    }

    public static void setSfxVolume(float volume) {
        sfxVolume = clamp(volume);
        applyVolumes();
    }

    private static void applyVolumes() {
        for (AudioTrack track : MUSIC_TRACKS) {
            track.applyGain(resolveGain(AudioTrack.Kind.MUSIC));
        }
        for (AudioTrack track : SFX_TRACKS) {
            track.applyGain(resolveGain(AudioTrack.Kind.SFX));
        }
    }

    private static void stopAll(boolean postEnd) {
        stopMusic(postEnd);
        for (AudioTrack track : SFX_TRACKS) {
            track.stop();
        }
    }

    private static AudioTrack ensureMusicTrack() {
        if (MUSIC_TRACKS.isEmpty()) {
            AudioTrack track = new AudioTrack(AudioTrack.Kind.MUSIC);
            MUSIC_TRACKS.add(track);
            return track;
        }
        return MUSIC_TRACKS.get(0);
    }

    private static AudioTrack findAvailableSfxTrack() {
        for (AudioTrack track : SFX_TRACKS) {
            if (!track.isActive()) {
                return track;
            }
        }
        AudioTrack track = new AudioTrack(AudioTrack.Kind.SFX);
        SFX_TRACKS.add(track);
        return track;
    }

    private static int bufferFor(ResourceLocation location) {
        return BUFFER_CACHE.computeIfAbsent(location, key -> {
            try (AudioData data = ResourceLoader.loadAudio(key)) {
                int format = resolveFormat(data);
                int bufferId = alGenBuffers();
                alBufferData(bufferId, format, data.pcm(), data.sampleRate());
                return bufferId;
            } catch (IOException ex) {
                LOGGER.error("Failed to load audio: {}", key, ex);
                throw new RuntimeException("Failed to load audio: " + key, ex);
            }
        });
    }

    private static int resolveFormat(AudioData data) {
        if (data.bitsPerSample() != 16) {
            throw new IllegalArgumentException("Only 16-bit PCM is supported");
        }
        return switch (data.channels()) {
            case 1 -> AL_FORMAT_MONO16;
            case 2 -> AL_FORMAT_STEREO16;
            default -> throw new IllegalArgumentException("Unsupported channel count: " + data.channels());
        };
    }

    private static float resolveGain(AudioTrack.Kind kind) {
        float category = kind == AudioTrack.Kind.MUSIC ? musicVolume : sfxVolume;
        return clamp(masterVolume * category);
    }

    private static float clamp(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    @EventSubscriber
    public static final class InitializationListener {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void onClientRegistriesInit(InitializationEvent event) {
            if (event.stage() != InitializationEvent.Stage.CLIENT_REGISTRIES) {
                return;
            }
            event.measure("audio", AudioManager::initialize);
        }
    }
}
