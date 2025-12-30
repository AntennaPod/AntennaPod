package de.danoeh.antennapod.ui.screen.feed;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.RemoveFeedDialogBinding;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBWriter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class RemoveFeedDialog extends BottomSheetDialogFragment {
    private static final String TAG = "RemoveFeedDialog";
    private static final String ARGUMENT_FEEDS = "feeds";

    protected List<Feed> feeds;
    private RemoveFeedDialogBinding binding;
    private Disposable disposable;

    public RemoveFeedDialog() {
        // Required empty public constructor
    }

    public RemoveFeedDialog(List<Feed> feeds) {
        Bundle args = new Bundle();
        args.putSerializable(ARGUMENT_FEEDS, new ArrayList<>(feeds));
        setArguments(args);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = RemoveFeedDialogBinding.inflate(inflater, container, false);
        if (getArguments() == null || !getArguments().containsKey(ARGUMENT_FEEDS)) {
            Log.e(TAG, "No feeds specified");
            dismiss();
            return binding.getRoot();
        }
        feeds = (List<Feed>) getArguments().getSerializable(ARGUMENT_FEEDS);
        if (feeds.size() == 1) {
            binding.selectionText.setText(feeds.get(0).getTitle());
        } else {
            binding.selectionText.setText(getResources()
                    .getQuantityString(R.plurals.num_subscriptions, feeds.size(), feeds.size()));
        }
        boolean allArchived = true;
        for (Feed feed : feeds) {
            if (feed.getState() != Feed.STATE_ARCHIVED) {
                allArchived = false;
                break;
            }
        }
        if (allArchived) {
            binding.archiveButton.setVisibility(View.GONE);
            binding.restoreButton.setVisibility(View.VISIBLE);
            binding.explanationArchiveText.setVisibility(View.GONE);
        }
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.removeButton.setOnClickListener(v -> showRemoveConfirm());
        binding.removeConfirmButton.setOnClickListener(v -> onRemoveButtonPressed());
        binding.archiveButton.setOnClickListener(v ->
                onArchiveButtonPressed(R.string.archiving_podcast_progress, Feed.STATE_ARCHIVED));
        binding.restoreButton.setOnClickListener(v ->
                onArchiveButtonPressed(R.string.restoring_podcast_progress, Feed.STATE_SUBSCRIBED));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (disposable != null) {
            disposable.dispose();
            disposable = null;
        }
        binding = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            setupFullHeight(bottomSheetDialog);
        });
        return dialog;
    }

    private void setupFullHeight(BottomSheetDialog bottomSheetDialog) {
        FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
            ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
            bottomSheet.setLayoutParams(layoutParams);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    protected void onRemoveButtonPressed() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.removeConfirmButton.setVisibility(View.GONE);
        binding.archiveButton.setVisibility(View.GONE);
        binding.cancelButton.setVisibility(View.GONE);

        disposable = Completable.fromAction(
                () -> {
                    for (int i = 0; i < feeds.size(); i++) {
                        Feed feed = feeds.get(i);
                        updateProgressText(R.string.deleting_podcast_progress, i + 1, feeds.size());
                        DBWriter.deleteFeed(context, feed.getId()).get();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "Feed(s) deleted");
                            dismiss();
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            dismiss();
                        });
    }

    private void onArchiveButtonPressed(int progressTextResId, int newState) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.removeButton.setVisibility(View.GONE);
        binding.archiveButton.setVisibility(View.GONE);
        binding.cancelButton.setVisibility(View.GONE);

        disposable = Completable.fromAction(
                () -> {
                    for (int i = 0; i < feeds.size(); i++) {
                        Feed feed = feeds.get(i);
                        updateProgressText(progressTextResId, i + 1, feeds.size());
                        DBWriter.setFeedState(context, feed, newState).get();
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "Feed(s) archived");
                            dismiss();
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            dismiss();
                        });
    }

    private void updateProgressText(int stringResId, int currentIndex, int total) {
        // Update UI on main thread if fragment is still attached
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (binding != null) {
                    String progressText = getString(stringResId, currentIndex, total);
                    binding.selectionText.setText(progressText);
                }
            });
        }
    }

    private void showRemoveConfirm() {
        binding.removeButton.setVisibility(View.GONE);
        binding.removeConfirmButton.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) binding.removeConfirmButton.getLayoutParams();
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 2.0f);
        animator.addUpdateListener(animation -> {
            params.weight = (float) animation.getAnimatedValue();
            binding.removeConfirmButton.setLayoutParams(params);
        });
        animator.setDuration(400);
        animator.setInterpolator(new OvershootInterpolator(3.0f));
        animator.start();
    }
}
