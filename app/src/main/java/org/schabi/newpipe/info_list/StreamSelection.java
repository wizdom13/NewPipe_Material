package org.schabi.newpipe.info_list;

import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selection survives row recycling and distinguishes the same URL on different services. */
public final class StreamSelection {
    private final Set<String> keys = new HashSet<>();

    private static String key(final StreamInfoItem item) {
        return item.getServiceId() + ":" + item.getUrl();
    }

    public boolean contains(final StreamInfoItem item) {
        return item != null && keys.contains(key(item));
    }

    public void toggle(final StreamInfoItem item) {
        if (!keys.remove(key(item))) {
            keys.add(key(item));
        }
    }

    public void selectAll(final List<StreamInfoItem> items) {
        items.forEach(item -> keys.add(key(item)));
    }

    public List<StreamInfoItem> itemsInDisplayOrder(final List<StreamInfoItem> items) {
        final List<StreamInfoItem> selected = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (final StreamInfoItem item : items) {
            if (contains(item) && seen.add(key(item))) {
                selected.add(item);
            }
        }
        return selected;
    }

    public void clear() {
        keys.clear();
    }
}
