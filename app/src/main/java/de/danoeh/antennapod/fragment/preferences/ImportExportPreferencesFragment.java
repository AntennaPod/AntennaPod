package de.danoeh.antennapod.fragment.preferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceFragmentCompat;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.ImportExportActivity;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.asynctask.DocumentFileExportWorker;
import de.danoeh.antennapod.asynctask.ExportWorker;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.export.html.HtmlWriter;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.util.List;

public class ImportExportPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "ImportExPrefFragment";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_OPML_IMPORT = "prefOpmlImport";
    private static final String PREF_HTML_EXPORT = "prefHtmlExport";
    private static final String IMPORT_EXPORT = "importExport";
    private static final int CHOOSE_OPML_EXPORT_PATH = 1;
    private static final String DEFAULT_OPML_OUTPUT_NAME = "antennapod-feeds.opml";
    private static final String CONTENT_TYPE_OPML = "text/x-opml";
    private static final int CHOOSE_HTML_EXPORT_PATH = 2;
    private static final String DEFAULT_HTML_OUTPUT_NAME = "antennapod-feeds.html";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private Disposable disposable;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_import_export);
        setupStorageScreen();
    }

    @Override
    public void onStart() {
        super.onStart();
        ((PreferenceActivity) getActivity()).getSupportActionBar().setTitle(R.string.import_export_pref);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void setupStorageScreen() {
        final Activity activity = getActivity();

        findPreference(IMPORT_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, ImportExportActivity.class));
                    return true;
                }
        );
        findPreference(PREF_OPML_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    openExportPathPicker(CONTENT_TYPE_OPML, DEFAULT_OPML_OUTPUT_NAME,
                            CHOOSE_OPML_EXPORT_PATH, new OpmlWriter());
                    return true;
                }
        );
        findPreference(PREF_HTML_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    openExportPathPicker(CONTENT_TYPE_HTML, DEFAULT_HTML_OUTPUT_NAME,
                            CHOOSE_HTML_EXPORT_PATH, new HtmlWriter());
                    return true;
                });
        findPreference(PREF_OPML_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, OpmlImportFromPathActivity.class));
                    return true;
                });
    }

    private boolean export(ExportWriter exportWriter, final Uri uri) {
        Context context = getActivity();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.exporting_label));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        if (uri == null) {
            Observable<File> observable = new ExportWorker(exportWriter, getContext()).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                context.getString(R.string.provider_authority), output);
                        showExportSuccessDialog(context.getString(R.string.export_success_sum, output.toString()), fileUri);
                    }, this::showExportErrorDialog, progressDialog::dismiss);
        } else {
            Observable<DocumentFile> observable = new DocumentFileExportWorker(exportWriter, context, uri).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        showExportSuccessDialog(context.getString(R.string.export_success_sum, output.getUri()), output.getUri());
                    }, this::showExportErrorDialog, progressDialog::dismiss);
        }
        return true;
    }

    private void showExportSuccessDialog(final String message, final Uri streamUri) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getContext())
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_success_title);
        alert.setMessage(message);
        alert.setPositiveButton(R.string.send_label, (dialog, which) -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.opml_export_label));
            sendIntent.putExtra(Intent.EXTRA_STREAM, streamUri);
            sendIntent.setType("text/plain");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = getContext().getPackageManager()
                        .queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    getContext().grantUriPermission(packageName, streamUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            getContext().startActivity(Intent.createChooser(sendIntent, getString(R.string.send_label)));
        });
        alert.create().show();
    }

    private void showExportErrorDialog(final Throwable error) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getContext())
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_error_label);
        alert.setMessage(error.getMessage());
        alert.show();
    }

    @SuppressLint("NewApi")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_OPML_EXPORT_PATH) {
            Uri uri = data.getData();
            export(new OpmlWriter(), uri);
        }

        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_HTML_EXPORT_PATH) {
            Uri uri = data.getData();
            export(new HtmlWriter(), uri);
        }
    }

    private void openExportPathPicker(String contentType, String title, int requestCode, ExportWriter writer) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Intent intentPickAction = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(contentType)
                    .putExtra(Intent.EXTRA_TITLE, title);

            // Creates an implicit intent to launch a file manager which lets
            // the user choose a specific directory to export to.
            try {
                startActivityForResult(intentPickAction, requestCode);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found. Should never happen...");
            }
        }

        // If we are using a SDK lower than API 21 or the implicit intent failed
        // fallback to the legacy export process
        export(writer, null);
    }
}
