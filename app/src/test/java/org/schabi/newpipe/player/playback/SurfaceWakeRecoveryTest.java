/*
 * SPDX-FileCopyrightText: 2026 WizeStream contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.player.playback;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.media3.common.Player;

import org.junit.Test;

public class SurfaceWakeRecoveryTest {
    @Test
    public void wakeRecoveryOnlyRebindsAValidSurface() {
        final Context context = mock(Context.class);
        final Player player = mock(Player.class);
        final SurfaceHolder holder = mock(SurfaceHolder.class);
        final Surface surface = mock(Surface.class);
        when(holder.getSurface()).thenReturn(surface);

        final SurfaceHolderCallback callback = new SurfaceHolderCallback(context, player);
        when(surface.isValid()).thenReturn(false);
        assertFalse(callback.rebindVideoSurfaceIfValid(holder));
        verify(player, never()).setVideoSurface(surface);

        when(surface.isValid()).thenReturn(true);
        assertTrue(callback.rebindVideoSurfaceIfValid(holder));
        verify(player).setVideoSurface(surface);
    }
}
