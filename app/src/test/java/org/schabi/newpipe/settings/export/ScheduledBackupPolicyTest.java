package org.schabi.newpipe.settings.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.documentfile.provider.DocumentFile;

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

    @Test
    public void retentionDeletesOnlyOlderManagedBackups() {
        final DocumentFile directory = mock(DocumentFile.class);
        final DocumentFile[] files = new DocumentFile[10];
        for (int index = 0; index < 9; index++) {
            files[index] = mock(DocumentFile.class);
            when(files[index].isFile()).thenReturn(true);
            when(files[index].getName())
                    .thenReturn("WizeStreamAuto-20260912_12000000" + index + ".zip");
        }
        files[9] = mock(DocumentFile.class);
        when(files[9].isFile()).thenReturn(true);
        when(files[9].getName()).thenReturn("WizeStreamData-manual-backup.zip");
        when(directory.listFiles()).thenReturn(files);

        ScheduledBackupWorker.pruneBackups(directory);

        verify(files[0]).delete();
        verify(files[1]).delete();
        for (int index = 2; index < files.length; index++) {
            verify(files[index], never()).delete();
        }
    }
}
