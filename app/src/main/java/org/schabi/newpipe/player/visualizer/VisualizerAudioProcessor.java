package org.schabi.newpipe.player.visualizer;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;
import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.function.LongSupplier;

/**
 * Pass-through PCM processor that exposes a small, normalized waveform for the player visualizer.
 * It operates on decoded audio and therefore does not require microphone access.
 */
public final class VisualizerAudioProcessor extends BaseAudioProcessor {
    public static final int SAMPLE_COUNT = 128;
    static final long FRAME_DURATION_NANOS = 1_000_000_000L / 60L;
    private static final int TARGET_FRAME_RATE = 60;
    private static final int MAX_QUEUED_FRAMES = TARGET_FRAME_RATE * 2;

    private final Object frameLock = new Object();
    private final ArrayDeque<float[]> queuedFrames = new ArrayDeque<>();
    private final LongSupplier nanoTimeSupplier;
    private float[] accumulatingSamples = new float[SAMPLE_COUNT];
    private float[] latestSamples = new float[SAMPLE_COUNT];
    private int accumulatedSampleCount;
    private long nextFrameTimeNanos;
    private volatile boolean enabled;

    /** Create a processor paced by the monotonic system clock. */
    public VisualizerAudioProcessor() {
        this(System::nanoTime);
    }

    VisualizerAudioProcessor(final LongSupplier nanoTimeSupplier) {
        this.nanoTimeSupplier = nanoTimeSupplier;
    }

    /**
     * Enable or disable waveform capture. Audio remains pass-through in both states.
     *
     * @param enabled whether waveform capture should run
     */
    public void setEnabled(final boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        synchronized (frameLock) {
            resetCapturedFrames();
        }
    }

    /**
     * Copy the latest waveform into a caller-owned array.
     *
     * @param target destination array
     * @return number of samples copied
     */
    public int copyLatestSamples(final float[] target) {
        synchronized (frameLock) {
            advanceVisibleFrame(nanoTimeSupplier.getAsLong());
            final int count = Math.min(target.length, latestSamples.length);
            System.arraycopy(latestSamples, 0, target, 0, count);
            return count;
        }
    }

    @Override
    protected AudioFormat onConfigure(final AudioFormat inputAudioFormat)
            throws AudioProcessor.UnhandledAudioFormatException {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET;
        }
        synchronized (frameLock) {
            accumulatingSamples = new float[Math.max(SAMPLE_COUNT,
                    inputAudioFormat.sampleRate / TARGET_FRAME_RATE)];
            resetCapturedFrames();
        }
        return inputAudioFormat;
    }

    @Override
    protected void onFlush() {
        synchronized (frameLock) {
            resetCapturedFrames();
        }
    }

    @Override
    protected void onReset() {
        synchronized (frameLock) {
            accumulatingSamples = new float[SAMPLE_COUNT];
            resetCapturedFrames();
        }
    }

    @Override
    public void queueInput(final ByteBuffer inputBuffer) {
        if (!inputBuffer.hasRemaining()) {
            return;
        }

        final int size = inputBuffer.remaining();
        if (enabled && size >= 2) {
            captureWaveform(inputBuffer);
        }
        final ByteBuffer outputBuffer = replaceOutputBuffer(size);
        outputBuffer.put(inputBuffer);
        outputBuffer.flip();
    }

    private void captureWaveform(final ByteBuffer inputBuffer) {
        final ByteBuffer samples = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        final int channelCount = Math.max(1, inputAudioFormat.channelCount);
        final int frameSize = channelCount * 2;

        synchronized (frameLock) {
            if (!enabled) {
                return;
            }
            while (samples.remaining() >= frameSize) {
                float mixed = 0.0f;
                for (int channel = 0; channel < channelCount; channel++) {
                    mixed += samples.getShort() / 32768.0f;
                }
                accumulatingSamples[accumulatedSampleCount++] = mixed / channelCount;
                if (accumulatedSampleCount == accumulatingSamples.length) {
                    enqueueAccumulatedFrame();
                }
            }
        }
    }

    private void enqueueAccumulatedFrame() {
        final float[] frame = new float[SAMPLE_COUNT];
        for (int outputIndex = 0; outputIndex < SAMPLE_COUNT; outputIndex++) {
            final int sampleIndex = Math.min(accumulatedSampleCount - 1,
                    outputIndex * accumulatedSampleCount / SAMPLE_COUNT);
            frame[outputIndex] = accumulatingSamples[sampleIndex];
        }
        if (queuedFrames.size() == MAX_QUEUED_FRAMES) {
            queuedFrames.removeFirst();
        }
        queuedFrames.addLast(frame);
        accumulatedSampleCount = 0;
    }

    private void advanceVisibleFrame(final long nowNanos) {
        if (queuedFrames.isEmpty()) {
            nextFrameTimeNanos = 0L;
            return;
        }
        if (nextFrameTimeNanos == 0L) {
            latestSamples = queuedFrames.removeFirst();
            nextFrameTimeNanos = nowNanos + FRAME_DURATION_NANOS;
            return;
        }
        if (nowNanos < nextFrameTimeNanos) {
            return;
        }

        final long dueFrameCount = 1L
                + (nowNanos - nextFrameTimeNanos) / FRAME_DURATION_NANOS;
        for (long frame = 0; frame < dueFrameCount && !queuedFrames.isEmpty(); frame++) {
            latestSamples = queuedFrames.removeFirst();
        }
        nextFrameTimeNanos += dueFrameCount * FRAME_DURATION_NANOS;
    }

    private void resetCapturedFrames() {
        queuedFrames.clear();
        accumulatedSampleCount = 0;
        latestSamples = new float[SAMPLE_COUNT];
        nextFrameTimeNanos = 0L;
    }
}
