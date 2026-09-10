package org.schabi.newpipe.util;

final class DesktopModeDetector {
    private static final int POINTER_MIN_SMALLEST_WIDTH_DP = 600;

    private DesktopModeDetector() {
    }

    static boolean shouldTreatCursorInputAsDesktop(final boolean cursorInputAvailable,
                                                   final int smallestScreenWidthDp) {
        return cursorInputAvailable
                && smallestScreenWidthDp >= POINTER_MIN_SMALLEST_WIDTH_DP;
    }
}
