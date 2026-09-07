package org.schabi.newpipe.player.pip;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.schabi.newpipe.player.pip.FloatingPlayerActionPreference.Action;

public class FloatingPlayerActionPreferenceTest {
    private static final String NATIVE_PIP_VALUE = "native_pip";

    @Test
    public void nativePipCanBeThePrimaryActionOnSupportedAndroidVersions() {
        assertEquals(Action.NATIVE_PIP, FloatingPlayerActionPreference.primaryAction(
                26, NATIVE_PIP_VALUE, NATIVE_PIP_VALUE));
        assertEquals(Action.POPUP, FloatingPlayerActionPreference.secondaryAction(
                Action.NATIVE_PIP));
    }

    @Test
    public void popupCanBeThePrimaryAction() {
        assertEquals(Action.POPUP, FloatingPlayerActionPreference.primaryAction(
                26, "popup_player", NATIVE_PIP_VALUE));
        assertEquals(Action.NATIVE_PIP, FloatingPlayerActionPreference.secondaryAction(
                Action.POPUP));
    }

    @Test
    public void popupRemainsPrimaryWhenNativePipIsUnavailable() {
        assertEquals(Action.POPUP, FloatingPlayerActionPreference.primaryAction(
                25, NATIVE_PIP_VALUE, NATIVE_PIP_VALUE));
    }
}
