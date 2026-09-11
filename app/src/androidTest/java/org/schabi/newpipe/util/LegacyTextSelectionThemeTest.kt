/*
 * SPDX-FileCopyrightText: 2026 WizeStream contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package org.schabi.newpipe.util

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.schabi.newpipe.R

@RunWith(AndroidJUnit4::class)
class LegacyTextSelectionThemeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun searchSuggestionThemesResolveFrameworkLinkTextColor() {
        listOf(R.style.LightTheme, R.style.DarkTheme, R.style.BlackTheme).forEach { theme ->
            val appContext = ContextThemeWrapper(context, theme)
            val suggestionContext = ContextThemeWrapper(appContext, R.style.SearchSuggestionTheme)
            val attributes = suggestionContext.obtainStyledAttributes(
                intArrayOf(android.R.attr.textColorLink)
            )

            try {
                assertNotNull(attributes.getColorStateList(0))
            } finally {
                attributes.recycle()
            }
        }
    }
}
