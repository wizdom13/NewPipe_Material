package org.schabi.newpipe;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MainActivityPlayerReceiverLifecycleTest {

    @Test
    public void registersOnlyWhileForegroundAndWaitingForPlayer() {
        assertTrue(MainActivity.shouldRegisterPlayerStartedReceiver(true, false, false));
        assertFalse(MainActivity.shouldRegisterPlayerStartedReceiver(false, false, false));
        assertFalse(MainActivity.shouldRegisterPlayerStartedReceiver(true, true, false));
        assertFalse(MainActivity.shouldRegisterPlayerStartedReceiver(true, false, true));
    }
}
