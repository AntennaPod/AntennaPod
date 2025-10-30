package de.danoeh.antennapod.ui.screen.rating;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.databinding.RatingDialogBinding;
import de.danoeh.antennapod.ui.common.DateFormatter;

import java.util.Date;

public class RatingDialogFragment extends DialogFragment {
    private static final String EXTRA_TOTAL_TIME = "totalTime";
    private static final String EXTRA_OLDEST_DATE = "oldestDate";

    public static RatingDialogFragment newInstance(long totalTime, long oldestDate) {
        RatingDialogFragment fragment = new RatingDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(EXTRA_TOTAL_TIME, totalTime);
        arguments.putLong(EXTRA_OLDEST_DATE, oldestDate);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(getContext())
                .setView(onCreateView(getLayoutInflater(), null, savedInstanceState))
                .create();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RatingDialogBinding viewBinding = RatingDialogBinding.inflate(inflater);
        long totalTime = getArguments().getLong(EXTRA_TOTAL_TIME, 0);
        long oldestDate = getArguments().getLong(EXTRA_OLDEST_DATE, 0);

        viewBinding.headerLabel.setText(HtmlCompat.fromHtml(getString(R.string.rating_tagline,
                DateFormatter.formatAbbrev(getContext(), new Date(oldestDate)),
                "<br/><b><big><big><big><big><big>", totalTime / 3600L,
                "</big></big></big></big></big></b><br/>"), HtmlCompat.FROM_HTML_MODE_LEGACY));
        viewBinding.neverAgainButton.setOnClickListener(v -> {
            new RatingDialogManager(getActivity()).saveRated();
            dismiss();
        });
        viewBinding.showLaterButton.setOnClickListener(v -> {
            new RatingDialogManager(getActivity()).resetStartDate();
            dismiss();
        });
        viewBinding.rateButton.setOnClickListener(v -> {
            IntentUtils.openInBrowser(getContext(),
                    "https://play.google.com/store/apps/details?id=de.danoeh.antennapod");
            new RatingDialogManager(getActivity()).saveRated();
        });
        viewBinding.contibuteButton.setOnClickListener(v -> {
            IntentUtils.openInBrowser(getContext(), IntentUtils.getLocalizedWebsiteLink(getContext()) + "/contribute/");
            new RatingDialogManager(getActivity()).saveRated();
        });
        return viewBinding.getRoot();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        new RatingDialogManager(getActivity()).resetStartDate();
    }
}
