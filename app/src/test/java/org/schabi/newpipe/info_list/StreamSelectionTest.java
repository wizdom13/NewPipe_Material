package org.schabi.newpipe.info_list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.util.List;

public class StreamSelectionTest {
    private static StreamInfoItem item(final int service, final String url) {
        return new StreamInfoItem(service, url, "Title", StreamType.VIDEO_STREAM);
    }

    @Test
    public void selectionUsesServiceAndUrlInsteadOfObjectIdentity() {
        final StreamSelection selection = new StreamSelection();
        selection.toggle(item(0, "url"));
        assertTrue(selection.contains(item(0, "url")));
        assertFalse(selection.contains(item(1, "url")));
        selection.toggle(item(0, "url"));
        assertFalse(selection.contains(item(0, "url")));
    }

    @Test
    public void actionsUseDisplayOrderAndIgnoreRemovedOrDuplicateRows() {
        final StreamInfoItem first = item(0, "first");
        final StreamInfoItem second = item(0, "second");
        final StreamSelection selection = new StreamSelection();
        selection.toggle(second);
        selection.toggle(first);
        selection.toggle(item(0, "removed"));
        assertEquals(List.of(first, second),
                selection.itemsInDisplayOrder(List.of(first, second, item(0, "first"))));
        selection.clear();
        assertTrue(selection.itemsInDisplayOrder(List.of(first, second)).isEmpty());
    }
}
