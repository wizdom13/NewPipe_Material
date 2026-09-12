package org.schabi.newpipe.settings.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ScheduledBackupPolicyTest {
    @Test
    public void unsupportedIntervalsDisableScheduling() {
        assertEquals(0, ScheduledBackupWorker.intervalDays(null));
        assertEquals(0, ScheduledBackupWorker.intervalDays("off"));
        assertEquals(0, ScheduledBackupWorker.intervalDays("unknown"));
        assertEquals(1, ScheduledBackupWorker.intervalDays("daily"));
        assertEquals(7, ScheduledBackupWorker.intervalDays("weekly"));
    }

    @Test
    public void retentionRecognizesOnlyAutomaticBackupFiles() {
        assertTrue(ScheduledBackupWorker.isManagedBackup("WizeStreamAuto-20260912_120030123.zip"));
        assertFalse(ScheduledBackupWorker.isManagedBackup("WizeStreamData-20260912_120030.zip"));
        assertFalse(ScheduledBackupWorker.isManagedBackup("WizeStreamAuto-important.zip"));
        assertFalse(ScheduledBackupWorker.isManagedBackup(null));
    }
}
