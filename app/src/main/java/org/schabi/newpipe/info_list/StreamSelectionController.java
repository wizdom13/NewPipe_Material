package org.schabi.newpipe.info_list;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import org.schabi.newpipe.R;
import org.schabi.newpipe.database.stream.model.StreamEntity;
import org.schabi.newpipe.download.BulkDownloadDialog;
import org.schabi.newpipe.download.BulkDownloadItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.local.dialog.PlaylistDialog;
import org.schabi.newpipe.player.playqueue.SinglePlayQueue;
import org.schabi.newpipe.util.NavigationHelper;

import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/** Shared contextual actions for remote lists and the local subscription feed. */
public final class StreamSelectionController implements ActionMode.Callback {
    private final Fragment fragment;
    private final RecyclerView recycler;
    private final Supplier<List<StreamInfoItem>> items;
    private final StreamSelection selection = new StreamSelection();
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final RecyclerView.ItemDecoration decoration;
    private ActionMode actionMode;

    public StreamSelectionController(final Fragment fragment, final RecyclerView recycler,
                                     final Supplier<List<StreamInfoItem>> items,
                                     final IntFunction<StreamInfoItem> itemAtPosition) {
        this.fragment = fragment;
        this.recycler = recycler;
        this.items = items;
        decoration = new RecyclerView.ItemDecoration() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public void onDrawOver(@NonNull final Canvas canvas,
                                   @NonNull final RecyclerView parent,
                                   @NonNull final RecyclerView.State state) {
                paint.setColor(ColorUtils.setAlphaComponent(MaterialColors.getColor(parent,
                        com.google.android.material.R.attr.colorPrimary), 50));
                for (int index = 0; index < parent.getChildCount(); index++) {
                    final View child = parent.getChildAt(index);
                    final int position = parent.getChildAdapterPosition(child);
                    final boolean selected = position != RecyclerView.NO_POSITION
                            && selection.contains(itemAtPosition.apply(position));
                    child.setSelected(selected);
                    if (selected) {
                        canvas.drawRoundRect(child.getLeft(), child.getTop(), child.getRight(),
                                child.getBottom(), 12, 12, paint);
                    }
                }
            }
        };
        recycler.addItemDecoration(decoration);
    }

    public void start(final StreamInfoItem item) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) fragment.requireActivity())
                    .startSupportActionMode(this);
        }
        if (actionMode != null && item != null) {
            selection.toggle(item);
        }
        refresh();
    }

    public boolean toggleIfActive(final StreamInfoItem item) {
        if (actionMode == null) {
            return false;
        }
        selection.toggle(item);
        refresh();
        return true;
    }

    public void refresh() {
        recycler.invalidateItemDecorations();
        if (actionMode != null) {
            actionMode.setTitle(fragment.getString(R.string.stream_selection_count,
                    selection.itemsInDisplayOrder(items.get()).size()));
            actionMode.invalidate();
        }
    }

    public void finish() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void destroy() {
        finish();
        recycler.removeItemDecoration(decoration);
        disposables.clear();
    }

    @Override
    public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
        menu.add(0, R.id.selection_all, 0, R.string.stream_select_all_loaded);
        menu.add(0, R.id.selection_enqueue, 1, R.string.enqueue_stream);
        menu.add(0, R.id.selection_playlist, 2, R.string.add_to_playlist);
        menu.add(0, R.id.selection_download, 3, R.string.download);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
        final boolean anySelected = !selection.itemsInDisplayOrder(items.get()).isEmpty();
        menu.findItem(R.id.selection_enqueue).setEnabled(anySelected);
        menu.findItem(R.id.selection_playlist).setEnabled(anySelected);
        menu.findItem(R.id.selection_download).setEnabled(anySelected);
        return true;
    }

    @Override
    public boolean onActionItemClicked(final ActionMode mode, final MenuItem action) {
        if (action.getItemId() == R.id.selection_all) {
            selection.selectAll(items.get());
            refresh();
            return true;
        }
        final List<StreamInfoItem> selected = selection.itemsInDisplayOrder(items.get());
        if (selected.isEmpty()) {
            return true;
        }
        if (action.getItemId() == R.id.selection_enqueue) {
            NavigationHelper.enqueueOnPlayer(fragment.requireContext(),
                    new SinglePlayQueue(selected, 0));
        } else if (action.getItemId() == R.id.selection_playlist) {
            disposables.add(PlaylistDialog.createCorrespondingDialog(fragment.requireContext(),
                    selected.stream().map(StreamEntity::new).collect(Collectors.toList()),
                    dialog -> dialog.show(fragment.getParentFragmentManager(), "PlaylistDialog")));
        } else if (action.getItemId() == R.id.selection_download) {
            BulkDownloadDialog.newInstance(selected.stream().map(BulkDownloadItem::from)
                    .collect(Collectors.toList()))
                    .show(fragment.getParentFragmentManager(), "BulkDownloadDialog");
        } else {
            return false;
        }
        mode.finish();
        return true;
    }

    @Override
    public void onDestroyActionMode(final ActionMode mode) {
        actionMode = null;
        selection.clear();
        recycler.invalidateItemDecorations();
    }
}
