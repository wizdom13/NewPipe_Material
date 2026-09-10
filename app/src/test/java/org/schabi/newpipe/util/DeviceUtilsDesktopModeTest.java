package org.schabi.newpipe.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceUtilsDesktopModeTest {
    @Test
    public void phonePointerDoesNotEnableDesktopMode() {
        assertFalse(DeviceUtils.shouldTreatCursorInputAsDesktop(true, 411));
        assertFalse(DeviceUtils.shouldTreatCursorInputAsDesktop(true, 599));
    }

    @Test
    public void largeScreenPointerCanEnableDesktopMode() {
        assertTrue(DeviceUtils.shouldTreatCursorInputAsDesktop(true, 600));
        assertTrue(DeviceUtils.shouldTreatCursorInputAsDesktop(true, 840));
    }

    @Test
    public void largeScreenWithoutPointerDoesNotUsePointerFallback() {
        assertFalse(DeviceUtils.shouldTreatCursorInputAsDesktop(false, 840));
    }
}
