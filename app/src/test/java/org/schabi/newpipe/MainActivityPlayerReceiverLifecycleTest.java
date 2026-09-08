package org.schabi.newpipe;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainActivityPlayerReceiverLifecycleTest {

    @Test
    public void playerStartedReceiverOnlyRunsWhileActivityIsForeground() throws Exception {
        final String source = Files.readString(
                Path.of("src/main/java/org/schabi/newpipe/MainActivity.java"),
                StandardCharsets.UTF_8
        );

        assertTrue(methodBody(source, "protected void onStart()")
                .contains("openMiniPlayerUponPlayerStarted();"));
        assertTrue(methodBody(source, "protected void onStop()")
                .contains("unregisterPlayerStartedReceiver();"));
        assertTrue(methodBody(source, "protected void onDestroy()")
                .contains("unregisterPlayerStartedReceiver();"));
        assertTrue(methodBody(source, "private void openMiniPlayerUponPlayerStarted()")
                .contains("if (broadcastReceiver != null)"));
    }

    private static String methodBody(final String source, final String signature) {
        final int start = source.indexOf(signature);
        final int nextMethod = source.indexOf("\n    @Override", start + signature.length());
        return source.substring(start, nextMethod < 0 ? source.length() : nextMethod);
    }
}
