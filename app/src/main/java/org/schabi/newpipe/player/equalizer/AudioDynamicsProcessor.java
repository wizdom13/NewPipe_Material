package org.schabi.newpipe.player.equalizer;

import androidx.media3.common.C;
import androidx.media3.common.audio.BaseAudioProcessor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.BooleanSupplier;

/** Adaptive RMS leveling and a stereo-linked compressor for decoded audio. */
public final class AudioDynamicsProcessor extends BaseAudioProcessor {
    public static final String NORMALIZATION_KEY = "audio_loudness_normalization";
    public static final String COMPRESSION_KEY = "audio_dynamic_compression";
    private static final double TARGET_RMS = 0.126; // approximately -18 dBFS
    private static final double NOISE_FLOOR = 0.00316; // -50 dBFS
    private static final double PEAK_CEILING = 0.98;

    private final BooleanSupplier normalize;
    private final BooleanSupplier compress;
    private double meanSquare;
    private double normalizationGain = 1.0;
    private double compressionGain = 1.0;
    private double rmsSmoothing;
    private double gainAttack;
    private double gainRelease;
    private double compressorAttack;
    private double compressorRelease;

    public AudioDynamicsProcessor(final BooleanSupplier normalize,
                                   final BooleanSupplier compress) {
        this.normalize = normalize;
        this.compress = compress;
    }

    @Override
    protected AudioFormat onConfigure(final AudioFormat format) {
        if (format.encoding != C.ENCODING_PCM_16BIT) {
            return AudioFormat.NOT_SET;
        }
        rmsSmoothing = coefficient(format.sampleRate, 0.4);
        gainAttack = coefficient(format.sampleRate, 0.2);
        gainRelease = coefficient(format.sampleRate, 3.0);
        compressorAttack = coefficient(format.sampleRate, 0.005);
        compressorRelease = coefficient(format.sampleRate, 0.15);
        return format;
    }

    private static double coefficient(final int sampleRate, final double seconds) {
        return Math.exp(-1.0 / (sampleRate * seconds));
    }

    @Override
    protected void onFlush() {
        meanSquare = 0.0;
        normalizationGain = 1.0;
        compressionGain = 1.0;
    }

    @Override
    public void queueInput(final ByteBuffer input) {
        if (!input.hasRemaining()) {
            return;
        }
        final boolean normalizationEnabled = normalize.getAsBoolean();
        final boolean compressionEnabled = compress.getAsBoolean();
        final ByteBuffer output = replaceOutputBuffer(input.remaining())
                .order(ByteOrder.LITTLE_ENDIAN);
        if (!normalizationEnabled && !compressionEnabled) {
            output.put(input).flip();
            onFlush();
            return;
        }
        final ByteBuffer samples = input.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        final int channels = inputAudioFormat.channelCount;
        final int frameBytes = channels * 2;
        while (samples.remaining() >= frameBytes) {
            final int start = samples.position();
            double sumSquares = 0.0;
            double peak = 0.0;
            for (int channel = 0; channel < channels; channel++) {
                final double sample = samples.getShort() / 32768.0;
                sumSquares += sample * sample;
                peak = Math.max(peak, Math.abs(sample));
            }
            meanSquare = rmsSmoothing * meanSquare
                    + (1.0 - rmsSmoothing) * sumSquares / channels;
            final double rms = Math.sqrt(meanSquare);
            final double targetGain = normalizationEnabled && rms > NOISE_FLOOR
                    ? Math.max(0.25, Math.min(4.0, TARGET_RMS / rms)) : 1.0;
            final double smoothing = targetGain < normalizationGain ? gainAttack : gainRelease;
            normalizationGain = smoothing * normalizationGain
                    + (1.0 - smoothing) * targetGain;
            final double levelGain = normalizationEnabled ? normalizationGain : 1.0;
            final double leveledPeak = peak * levelGain;
            // A 4:1 ratio above -18 dBFS, shared by all channels to preserve stereo balance.
            final double targetCompression = compressionEnabled && leveledPeak > TARGET_RMS
                    ? Math.pow(TARGET_RMS / leveledPeak, 0.75) : 1.0;
            final double compressionSmoothing = targetCompression < compressionGain
                    ? compressorAttack : compressorRelease;
            compressionGain = compressionSmoothing * compressionGain
                    + (1.0 - compressionSmoothing) * targetCompression;
            double gain = levelGain * (compressionEnabled ? compressionGain : 1.0);
            if (peak > 0) {
                gain = Math.min(gain, PEAK_CEILING / peak);
            }
            samples.position(start);
            for (int channel = 0; channel < channels; channel++) {
                output.putShort((short) Math.round(samples.getShort() * gain));
            }
        }
        // Media3 supplies complete PCM frames. Preserve unexpected trailing bytes safely.
        output.put(samples);
        input.position(input.limit());
        output.flip();
    }
}
