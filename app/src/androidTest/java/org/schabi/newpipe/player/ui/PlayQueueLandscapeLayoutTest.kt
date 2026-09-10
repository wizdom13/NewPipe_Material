package org.schabi.newpipe.player.ui

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.schabi.newpipe.R

@RunWith(AndroidJUnit4::class)
class PlayQueueLandscapeLayoutTest {
    @Test
    fun landscapePlaybackQueueIncludesHiddenVisualizer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val targetContext = instrumentation.targetContext
            val landscapeConfiguration =
                Configuration(targetContext.resources.configuration).apply {
                    orientation = Configuration.ORIENTATION_LANDSCAPE
                    screenWidthDp = 800
                    screenHeightDp = 450
                }
            val landscapeContext =
                targetContext.createConfigurationContext(landscapeConfiguration)
            val themedContext = ContextThemeWrapper(landscapeContext, R.style.LightTheme)
            val root = LayoutInflater.from(themedContext).inflate(
                R.layout.activity_player_queue_control,
                FrameLayout(themedContext),
                false
            )

            assertNotNull(root.findViewById<View>(R.id.play_queue))
            val visualizer = root.findViewById<View>(R.id.audio_visualizer)
            assertNotNull(visualizer)
            assertEquals(View.GONE, visualizer.visibility)
        }
    }
}
