package de.danoeh.antennapod.ui.preferences.screen.bugreport;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.AnimatedFragment;
import de.danoeh.antennapod.ui.common.ClipboardUtils;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.preferences.R;
import de.danoeh.antennapod.ui.preferences.databinding.BugReportFragmentBinding;

/**
 * UI fragment to allow the user to submit a bug report via the AntennaPod forum or GitHub page.
 */
public class BugReportFragment extends AnimatedFragment {

    private BugReportFragmentBinding viewBinding;
    private BugReportViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BugReportViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = BugReportFragmentBinding.inflate(inflater, container, false);

        return viewBinding.getRoot();
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupContextMenu();
        setupClipboardCopy();

        viewModel.getState().observe(getViewLifecycleOwner(), uiState -> {
            refreshEnvironmentInfo(uiState.getEnvironmentInfo());
            refreshCrashLogInfo(uiState);
        });

        viewBinding.btnExpandCrashLog.setOnClickListener(v -> {
            switch (viewModel.requireCurrentState().getCrashLogState()) {
                case SHOWN_COLLAPSED:
                    viewModel.setCrashLogState(BugReportViewModel.UiState.CrashLogState.SHOWN_EXPANDED);
                    break;

                case SHOWN_EXPANDED:
                    viewModel.setCrashLogState(BugReportViewModel.UiState.CrashLogState.SHOWN_COLLAPSED);
                    break;

                default:    // UNAVAILABLE
                    break;
            }
        });

        viewBinding.btnOpenForum.setOnClickListener(v -> {
            IntentUtils.openInBrowser(requireContext(), "https://forum.antennapod.org/search");
        });

        viewBinding.btnOpenGithub.setOnClickListener(v -> {
            IntentUtils.openInBrowser(requireContext(), "https://github.com/AntennaPod/AntennaPod/issues");
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setTitle(R.string.report_bug_title);
    }

    private void refreshEnvironmentInfo(@NonNull BugReportViewModel.EnvironmentInfo info) {
        viewBinding.txtAttribAppVersion.setText(info.applicationVersion);
        viewBinding.txtAttribAndroidVersion.setText(info.androidVersion);
        viewBinding.txtAttribDeviceModel.setText(info.deviceModel);
        viewBinding.txtAttribDeviceName.setText(info.deviceName);
        viewBinding.txtAttribProduct.setText(info.productName);
    }

    private void refreshCrashLogInfo(@NonNull BugReportViewModel.UiState uiState) {
        BugReportViewModel.UiState.CrashLogState state = uiState.getCrashLogState();
        BugReportViewModel.CrashLogInfo crashLogInfo = uiState.getCrashLogInfo();

        switch (state) {
            case SHOWN_COLLAPSED:
            case SHOWN_EXPANDED:
                viewBinding.txtCrashLog.setText(crashLogInfo.getContent());
                viewBinding.txtCrashLogMessage.setText(getString(
                        R.string.report_bug_crash_log_message, uiState.getFormattedCrashLogTimestamp()));

                if (state == BugReportViewModel.UiState.CrashLogState.SHOWN_COLLAPSED) {
                    viewBinding.btnExpandCrashLog.setText(R.string.report_bug_crash_log_expand);
                    viewBinding.txtCrashLog.setMinLines(4);
                    viewBinding.txtCrashLog.setMaxLines(4);
                } else {
                    viewBinding.btnExpandCrashLog.setText(R.string.report_bug_crash_log_collapse);
                    viewBinding.txtCrashLog.setMinLines(0);
                    viewBinding.txtCrashLog.setMaxLines(Integer.MAX_VALUE);
                }

                viewBinding.grpCrashLog.setVisibility(View.VISIBLE);
                break;

            default:    // UNAVAILABLE
                viewBinding.grpCrashLog.setVisibility(View.GONE);
                break;
        }
    }

    private void setupContextMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.bug_report_options, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.export_logcat) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());

                    builder.setTitle(menuItem.getTitle());
                    builder.setMessage(R.string.confirm_export_log_dialog_message);
                    builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                        exportLogcat();
                        dialog.dismiss();
                    });

                    builder.setNegativeButton(R.string.cancel_label, null);
                    builder.show();

                    return true;
                }

                return false;
            }
        }, getViewLifecycleOwner());
    }

    private void setupClipboardCopy() {
        Context ctx = requireContext();

        viewBinding.txtAttribAppVersion.setOnClickListener(
                new ClipboardUtils.TextViewCopyOnClickListener(ctx, R.string.report_bug_attrib_app_version));
        viewBinding.txtAttribAndroidVersion.setOnClickListener(
                new ClipboardUtils.TextViewCopyOnClickListener(ctx, R.string.report_bug_attrib_android_version));
        viewBinding.txtAttribDeviceModel.setOnClickListener(
                new ClipboardUtils.TextViewCopyOnClickListener(ctx, R.string.report_bug_attrib_device_model));
        viewBinding.txtAttribDeviceName.setOnClickListener(
                new ClipboardUtils.TextViewCopyOnClickListener(ctx, R.string.report_bug_attrib_device_name));
        viewBinding.txtAttribProduct.setOnClickListener(
                new ClipboardUtils.TextViewCopyOnClickListener(ctx, R.string.report_bug_attrib_product));

        viewBinding.txtCrashLog.setOnClickListener(new ClipboardUtils.ViewCopyOnClickListener(ctx,
                R.string.report_bug_crash_log_title, R.string.copied_to_clipboard) {
            @Override
            protected String getText(View view) {
                return viewModel.requireCurrentState().getCrashInfoWithMarkup();
            }
        });

        viewBinding.btnCopyToClipboard.setOnClickListener(new ClipboardUtils.ViewCopyOnClickListener(ctx,
                R.string.report_bug_title, R.string.copied_to_clipboard) {
            @Override
            protected String getText(View view) {
                return viewModel.requireCurrentState().getBugReportWithMarkup();
            }
        });
    }

    private void exportLogcat() {
        try {
            File filename = new File(UserPreferences.getDataFolder(null), "full-logs.txt");
            String cmd = "logcat -d -f " + filename.getAbsolutePath();
            Runtime.getRuntime().exec(cmd);

            //share file
            try {
                String authority = getString(R.string.provider_authority);
                Uri fileUri = FileProvider.getUriForFile(requireContext(), authority, filename);

                new ShareCompat.IntentBuilder(requireContext())
                        .setType("text/*")
                        .addStream(fileUri)
                        .setChooserTitle(R.string.share_file_label)
                        .startChooser();

            } catch (Exception e) {
                e.printStackTrace();
                Snackbar.make(viewBinding.getRoot(), R.string.log_file_share_exception, Snackbar.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();

            Snackbar.make(viewBinding.getRoot(), e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }
}