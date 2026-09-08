package org.schabi.newpipe.player.mediasession;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MediaSessionNotificationActionsTest {
    @Test
    public void refreshesActionsWhenAndroidSystemUiFinishesConnecting() {
        assertTrue(MediaSessionPlayerUi.shouldRefreshNotificationActions(true, true));
    }

    @Test
    public void ignoresNonNotificationAndPreAndroid13Controllers() {
        assertFalse(MediaSessionPlayerUi.shouldRefreshNotificationActions(true, false));
        assertFalse(MediaSessionPlayerUi.shouldRefreshNotificationActions(false, true));
        assertFalse(MediaSessionPlayerUi.shouldRefreshNotificationActions(false, false));
    }
}
