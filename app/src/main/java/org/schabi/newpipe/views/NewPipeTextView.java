package org.schabi.newpipe.views;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.text.method.MovementMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.preference.PreferenceManager;

import org.schabi.newpipe.R;
import org.schabi.newpipe.player.pip.FloatingPlayerActionPreference;
import org.schabi.newpipe.player.pip.FloatingPlayerActionPreference.Action;
import org.schabi.newpipe.player.pip.NativePipController;
import org.schabi.newpipe.util.NewPipeTextViewHelper;
import org.schabi.newpipe.util.external_communication.ShareUtils;

/**
 * An {@link AppCompatTextView} which uses {@link ShareUtils#shareText(Context, String, String)}
 * when sharing selected text by using the {@code Share} command of the floating actions.
 *
 * <p>
 * This class allows NewPipe to show Android share sheet instead of EMUI share sheet when sharing
 * text from {@link AppCompatTextView} on EMUI devices and also to keep movement method set when a
 * text change occurs, if the text cannot be selected and text links are clickable.
 * </p>
 */
public class NewPipeTextView extends AppCompatTextView {
    @Nullable
    private OnClickListener popupClickListener;
    @Nullable
    private OnLongClickListener popupLongClickListener;
    @Nullable
    private OnTouchListener popupTouchListener;

    public NewPipeTextView(@NonNull final Context context) {
        super(context);
    }

    public NewPipeTextView(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
    }

    public NewPipeTextView(@NonNull final Context context,
                           @Nullable final AttributeSet attrs,
                           final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(final CharSequence text, final BufferType type) {
        // We need to set again the movement method after a text change because Android resets the
        // movement method to the default one in the case where the text cannot be selected and
        // text links are clickable (which is the default case in NewPipe).
        final MovementMethod movementMethod = this.getMovementMethod();
        super.setText(text, type);
        setMovementMethod(movementMethod);
    }

    @Override
    public void setOnClickListener(@Nullable final OnClickListener listener) {
        if (!isPrimaryDetailPipAction()) {
            super.setOnClickListener(listener);
            return;
        }

        popupClickListener = listener;
        configureFloatingPlayerActions();
    }

    @Override
    public void setOnLongClickListener(@Nullable final OnLongClickListener listener) {
        if (!isPrimaryDetailPipAction()) {
            super.setOnLongClickListener(listener);
            return;
        }

        popupLongClickListener = listener;
        configureFloatingPlayerActions();
    }

    @Override
    public void setOnTouchListener(@Nullable final OnTouchListener listener) {
        if (!isPrimaryDetailPipAction()) {
            super.setOnTouchListener(listener);
            return;
        }

        popupTouchListener = listener;
        configureFloatingPlayerActions();
    }

    @Override
    public void setVisibility(final int visibility) {
        super.setVisibility(visibility);
        if (isPrimaryDetailPipAction()) {
            post(this::configureFloatingPlayerActions);
        }
    }

    @Override
    protected void onWindowVisibilityChanged(final int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE && isPrimaryDetailPipAction()) {
            post(this::configureFloatingPlayerActions);
        }
    }

    private boolean isPrimaryDetailPipAction() {
        return getId() == R.id.detail_controls_popup;
    }

    private boolean enterNativePictureInPicture() {
        Context current = getContext();
        while (current instanceof ContextWrapper) {
            if (current instanceof AppCompatActivity) {
                return new NativePipController((AppCompatActivity) current)
                        .enterPictureInPicture();
            }
            final Context base = ((ContextWrapper) current).getBaseContext();
            if (base == current) {
                break;
            }
            current = base;
        }
        return false;
    }

    private void configureFloatingPlayerActions() {
        if (!isPrimaryDetailPipAction()) {
            return;
        }

        final Action primaryAction = getPrimaryFloatingPlayerAction();
        configureAction(this, primaryAction, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureSecondaryFloatingPlayerAction(primaryAction);
        } else {
            final View secondaryAction = getRootView().findViewById(
                    R.id.detail_controls_secondary_floating_player);
            if (secondaryAction != null) {
                secondaryAction.setVisibility(View.GONE);
            }
        }
    }

    @NonNull
    private Action getPrimaryFloatingPlayerAction() {
        final String nativePipValue = getContext().getString(
                R.string.primary_floating_player_action_pip_value);
        final String configuredValue = PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getString(getContext().getString(
                        R.string.primary_floating_player_action_key), nativePipValue);
        return FloatingPlayerActionPreference.primaryAction(
                Build.VERSION.SDK_INT, configuredValue, nativePipValue);
    }

    private void ensureSecondaryFloatingPlayerAction(@NonNull final Action primaryAction) {
        final View root = getRootView();
        final LinearLayout secondaryControls =
                root.findViewById(R.id.detail_secondary_control_panel);
        if (secondaryControls == null) {
            return;
        }

        View secondaryAction = root.findViewById(R.id.detail_controls_secondary_floating_player);
        if (secondaryAction == null) {
            secondaryAction = LayoutInflater.from(getContext()).inflate(
                    R.layout.detail_legacy_popup_action, secondaryControls, false);
            secondaryControls.addView(secondaryAction, 0);
        }
        configureAction((NewPipeTextView) secondaryAction,
                FloatingPlayerActionPreference.secondaryAction(primaryAction), false);
        secondaryAction.setVisibility(getVisibility());
        if (getVisibility() == View.VISIBLE) {
            final View secondaryToggle = root.findViewById(
                    R.id.detail_toggle_secondary_controls_view);
            if (secondaryToggle != null) {
                secondaryToggle.setVisibility(View.VISIBLE);
            }
        }
    }

    private void configureAction(@NonNull final NewPipeTextView actionView,
                                 @NonNull final Action action,
                                 final boolean allowPopupFallback) {
        if (action == Action.NATIVE_PIP) {
            actionView.setText(R.string.controls_pip_title);
            actionView.setContentDescription(
                    getContext().getString(R.string.enter_picture_in_picture));
            actionView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, R.drawable.ic_picture_in_picture, 0, 0);
            actionView.superSetOnClickListener(view -> {
                if (!enterNativePictureInPicture()
                        && allowPopupFallback && popupClickListener != null) {
                    popupClickListener.onClick(view);
                }
            });
            actionView.superSetOnLongClickListener(null);
            actionView.superSetOnTouchListener(null);
        } else {
            actionView.setText(R.string.controls_popup_title);
            actionView.setContentDescription(getContext().getString(R.string.open_in_popup_mode));
            actionView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    0, R.drawable.ic_smart_display, 0, 0);
            actionView.superSetOnClickListener(popupClickListener);
            actionView.superSetOnLongClickListener(popupLongClickListener);
            actionView.superSetOnTouchListener(popupTouchListener);
        }
    }

    private void superSetOnClickListener(@Nullable final OnClickListener listener) {
        super.setOnClickListener(listener);
    }

    private void superSetOnLongClickListener(@Nullable final OnLongClickListener listener) {
        super.setOnLongClickListener(listener);
    }

    private void superSetOnTouchListener(@Nullable final OnTouchListener listener) {
        super.setOnTouchListener(listener);
    }

    @Override
    public boolean onTextContextMenuItem(final int id) {
        if (id == android.R.id.shareText) {
            NewPipeTextViewHelper.shareSelectedTextWithShareUtils(this);
            return true;
        }
        return super.onTextContextMenuItem(id);
    }
}
