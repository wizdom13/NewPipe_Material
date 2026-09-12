package org.schabi.newpipe.player.equalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.common.audio.AudioProcessor;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioDynamicsProcessorTest {
    private static AudioDynamicsProcessor processor(final boolean normalization,
                                                     final boolean compression) throws Exception {
        final AudioDynamicsProcessor processor =
                new AudioDynamicsProcessor(() -> normalization, () -> compression);
        processor.configure(new AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT));
        processor.flush();
        return processor;
    }

    private static ByteBuffer render(final AudioDynamicsProcessor processor,
                                      final short left, final short right, final int seconds) {
        final ByteBuffer input = ByteBuffer.allocateDirect(48000 * seconds * 4)
                .order(ByteOrder.LITTLE_ENDIAN);
        while (input.hasRemaining()) {
            input.putShort(left).putShort(right);
        }
        input.flip();
        processor.queueInput(input);
        assertEquals(0, input.remaining());
        return processor.getOutput().order(ByteOrder.LITTLE_ENDIAN);
    }

    @Test
    public void disabledProcessingIsBitExact() throws Exception {
        final ByteBuffer output = render(processor(false, false), Short.MIN_VALUE,
                Short.MAX_VALUE, 1);
        while (output.hasRemaining()) {
            assertEquals(Short.MIN_VALUE, output.getShort());
            assertEquals(Short.MAX_VALUE, output.getShort());
        }
    }

    @Test
    public void silenceStaysSilentAndEmptyBuffersAreAccepted() throws Exception {
        final AudioDynamicsProcessor processor = processor(true, true);
        processor.queueInput(AudioProcessor.EMPTY_BUFFER);
        assertEquals(0, processor.getOutput().remaining());
        final ByteBuffer output = render(processor, (short) 0, (short) 0, 1);
        while (output.hasRemaining()) {
            assertEquals(0, output.getShort());
        }
    }

    @Test
    public void normalizationRaisesQuietAndReducesLoudAudio() throws Exception {
        final ByteBuffer quiet = render(processor(true, false), (short) 1500, (short) 1500, 10);
        final ByteBuffer loud = render(processor(true, false), (short) 16000, (short) 16000, 10);
        assertTrue(quiet.getShort(quiet.limit() - 2) > 3500);
        assertTrue(loud.getShort(loud.limit() - 2) < 5000);
    }

    @Test
    public void compressionPreservesChannelBalance() throws Exception {
        final ByteBuffer output = render(processor(false, true), (short) 24000,
                (short) -12000, 2);
        final short left = output.getShort(output.limit() - 4);
        final short right = output.getShort(output.limit() - 2);
        assertTrue(left > 0 && left < 10000);
        assertEquals(left, -2 * right, 2.0);
    }

    @Test
    public void abruptPeaksCannotOverflowAfterQuietAudio() throws Exception {
        final AudioDynamicsProcessor processor = processor(true, false);
        render(processor, (short) 1000, (short) 1000, 10);
        final ByteBuffer output = render(processor, Short.MAX_VALUE, Short.MIN_VALUE, 1);
        while (output.hasRemaining()) {
            final short left = output.getShort();
            final short right = output.getShort();
            assertTrue(left > 0 && left <= 32113);
            assertTrue(right < 0 && right >= -32113);
        }
    }
}
