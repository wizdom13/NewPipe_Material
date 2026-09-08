package org.schabi.newpipe.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.exoplayer.hls.playlist.HlsPlaylistTracker;

import org.junit.Test;

import java.io.IOException;

public class PlayerHlsRecoveryTest {
    @Test
    public void treatsNestedStuckHlsPlaylistAsRecoverable() {
        final Throwable error = new RuntimeException("source", new IOException("network",
                new HlsPlaylistTracker.PlaylistStuckException(null)));

        assertTrue(PlayerHttpErrorRecovery.hasPlaylistStuckCause(error));
        assertTrue(PlayerHttpErrorRecovery.isRecoverableMediaUrlFailure(error));
        assertFalse(PlayerHttpErrorRecovery.hasPlaylistStuckCause(
                new RuntimeException("source", new IOException("network"))));
    }
}
