package org.schabi.newpipe.player.visualizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicLong;

public class VisualizerAudioProcessorTest {
    @Test
    public void processorAcceptsSharedEmptyInput() throws Exception {
        final VisualizerAudioProcessor processor = new VisualizerAudioProcessor();
        processor.configure(new AudioProcessor.AudioFormat(48_000, 2,
                C.ENCODING_PCM_16BIT));
        processor.flush();

        processor.queueInput(AudioProcessor.EMPTY_BUFFER);

        assertEquals(0, processor.getOutput().remaining());
    }

    @Test
    public void processorPassesAudioThroughAndCapturesWaveform() throws Exception {
        final VisualizerAudioProcessor processor = new VisualizerAudioProcessor();
        processor.setEnabled(true);
        processor.configure(new AudioProcessor.AudioFormat(48_000, 2,
                C.ENCODING_PCM_16BIT));
        processor.flush();

        final ByteBuffer input = ByteBuffer.allocateDirect(3_200)
                .order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < 800; i++) {
            input.putShort((short) 16_384);
            input.putShort((short) 16_384);
        }
        input.flip();
        processor.queueInput(input);

        assertEquals(3_200, processor.getOutput().remaining());
        final float[] waveform = new float[VisualizerAudioProcessor.SAMPLE_COUNT];
        assertEquals(waveform.length, processor.copyLatestSamples(waveform));
        assertTrue(waveform[0] > 0.49f);
    }

    @Test
    public void largeMedia3BufferIsPresentedAsPacedVisualizerFrames() throws Exception {
        final AtomicLong clock = new AtomicLong(1L);
        final VisualizerAudioProcessor processor = new VisualizerAudioProcessor(clock::get);
        processor.setEnabled(true);
        processor.configure(new AudioProcessor.AudioFormat(48_000, 2,
                C.ENCODING_PCM_16BIT));
        processor.flush();

        final ByteBuffer input = ByteBuffer.allocateDirect(6_400)
                .order(ByteOrder.LITTLE_ENDIAN);
        putStereoFrames(input, 800, (short) 16_384);
        putStereoFrames(input, 800, (short) -16_384);
        input.flip();
        processor.queueInput(input);

        final float[] firstFrame = new float[VisualizerAudioProcessor.SAMPLE_COUNT];
        final float[] sameDisplayFrame = new float[VisualizerAudioProcessor.SAMPLE_COUNT];
        final float[] secondFrame = new float[VisualizerAudioProcessor.SAMPLE_COUNT];
        processor.copyLatestSamples(firstFrame);
        processor.copyLatestSamples(sameDisplayFrame);
        clock.addAndGet(VisualizerAudioProcessor.FRAME_DURATION_NANOS);
        processor.copyLatestSamples(secondFrame);

        assertTrue(firstFrame[0] > 0.49f);
        assertEquals(firstFrame[0], sameDisplayFrame[0], 0.0f);
        assertTrue(secondFrame[0] < -0.49f);
    }

    @Test
    public void disablingProcessorClearsCapturedFrames() throws Exception {
        final VisualizerAudioProcessor processor = new VisualizerAudioProcessor();
        processor.setEnabled(true);
        processor.configure(new AudioProcessor.AudioFormat(48_000, 2,
                C.ENCODING_PCM_16BIT));
        processor.flush();

        final ByteBuffer input = ByteBuffer.allocateDirect(3_200)
                .order(ByteOrder.LITTLE_ENDIAN);
        putStereoFrames(input, 800, (short) 16_384);
        input.flip();
        processor.queueInput(input);
        processor.setEnabled(false);

        final float[] waveform = new float[VisualizerAudioProcessor.SAMPLE_COUNT];
        processor.copyLatestSamples(waveform);
        for (final float sample : waveform) {
            assertEquals(0.0f, sample, 0.0f);
        }
    }

    private static void putStereoFrames(final ByteBuffer input, final int count,
                                        final short sample) {
        for (int frame = 0; frame < count; frame++) {
            input.putShort(sample);
            input.putShort(sample);
        }
    }
}
