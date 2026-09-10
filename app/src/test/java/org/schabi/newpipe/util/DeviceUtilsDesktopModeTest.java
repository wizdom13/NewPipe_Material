package org.schabi.newpipe.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeviceUtilsDesktopModeTest {
    @Test
    public void phonePointerDoesNotEnableDesktopMode() {
        assertFalse(DesktopModeDetector.shouldTreatCursorInputAsDesktop(true, 411));
        assertFalse(DesktopModeDetector.shouldTreatCursorInputAsDesktop(true, 599));
    }

    @Test
    public void largeScreenPointerCanEnableDesktopMode() {
        assertTrue(DesktopModeDetector.shouldTreatCursorInputAsDesktop(true, 600));
        assertTrue(DesktopModeDetector.shouldTreatCursorInputAsDesktop(true, 840));
    }

    @Test
    public void largeScreenWithoutPointerDoesNotUsePointerFallback() {
        assertFalse(DesktopModeDetector.shouldTreatCursorInputAsDesktop(false, 840));
    }
}
