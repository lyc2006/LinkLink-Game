package io.github.theflysong.data.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;

public final class AudioData implements AutoCloseable {
    private final ByteBuffer pcm;
    private final int channels;
    private final int sampleRate;
    private final int bitsPerSample;

    private AudioData(ByteBuffer pcm, int channels, int sampleRate, int bitsPerSample) {
        this.pcm = pcm;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.bitsPerSample = bitsPerSample;
    }

    public static AudioData fromOgg(ByteBuffer oggData, String name) throws IOException {
        if (oggData == null) {
            throw new IOException("Audio data is null: " + name);
        }

        IntBuffer channelsBuf = BufferUtils.createIntBuffer(1);
        IntBuffer sampleRateBuf = BufferUtils.createIntBuffer(1);
        ShortBuffer decoded = stb_vorbis_decode_memory(oggData, channelsBuf, sampleRateBuf);
        if (decoded == null) {
            throw new IOException("Failed to decode OGG: " + name);
        }

        int channels = channelsBuf.get(0);
        int sampleRate = sampleRateBuf.get(0);
        if (channels > 2) {
            MemoryUtil.memFree(decoded);
            throw new IOException("Unsupported channel count: " + channels + " for " + name);
        }

        ByteBuffer pcm = MemoryUtil.memAlloc(decoded.remaining() * 2);
        pcm.asShortBuffer().put(decoded);
        MemoryUtil.memFree(decoded);
        return new AudioData(pcm, channels, sampleRate, 16);
    }

    public static AudioData fromWav(InputStream input, String name) throws IOException {
        if (input == null) {
            throw new IOException("Audio stream is null: " + name);
        }

        try (AudioInputStream stream = AudioSystem.getAudioInputStream(input)) {
            AudioFormat base = stream.getFormat();
            AudioFormat target = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    base.getSampleRate(),
                    16,
                    base.getChannels(),
                    base.getChannels() * 2,
                    base.getSampleRate(),
                    false);

            if (target.getChannels() > 2) {
                throw new IOException("Unsupported channel count: " + target.getChannels() + " for " + name);
            }

            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(target, stream)) {
                byte[] bytes = pcmStream.readAllBytes();
                ByteBuffer pcm = MemoryUtil.memAlloc(bytes.length);
                pcm.put(bytes);
                pcm.flip();
                return new AudioData(pcm, target.getChannels(), (int) target.getSampleRate(), 16);
            }
        } catch (UnsupportedAudioFileException ex) {
            throw new IOException("Unsupported audio format: " + name, ex);
        }
    }

    public ByteBuffer pcm() {
        return pcm;
    }

    public int channels() {
        return channels;
    }

    public int sampleRate() {
        return sampleRate;
    }

    public int bitsPerSample() {
        return bitsPerSample;
    }

    @Override
    public void close() {
        if (pcm != null) {
            MemoryUtil.memFree(pcm);
        }
    }
}
