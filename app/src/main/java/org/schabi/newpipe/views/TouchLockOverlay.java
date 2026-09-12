package org.schabi.newpipe.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Shields playback gestures while allowing the child unlock button to receive clicks. */
public final class TouchLockOverlay extends FrameLayout {
    public TouchLockOverlay(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        super.dispatchTouchEvent(event);
        return true;
    }
}
