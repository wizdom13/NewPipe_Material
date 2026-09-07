package org.schabi.newpipe.player.pip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Resolves which floating-player action is shown in the primary detail control row. */
public final class FloatingPlayerActionPreference {
    private static final int MINIMUM_NATIVE_PIP_SDK = 26;

    private FloatingPlayerActionPreference() {
    }

    public enum Action {
        NATIVE_PIP,
        POPUP
    }

    @NonNull
    public static Action primaryAction(final int sdkInt,
                                       @Nullable final String configuredValue,
                                       @NonNull final String nativePipValue) {
        return sdkInt >= MINIMUM_NATIVE_PIP_SDK && nativePipValue.equals(configuredValue)
                ? Action.NATIVE_PIP : Action.POPUP;
    }

    @NonNull
    public static Action secondaryAction(@NonNull final Action primaryAction) {
        return primaryAction == Action.NATIVE_PIP ? Action.POPUP : Action.NATIVE_PIP;
    }
}
