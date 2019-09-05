package de.danoeh.antennapod.fragment.preferences;

import android.Manifest;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.DirectoryChooserActivity;
import de.danoeh.antennapod.activity.ImportExportActivity;
import de.danoeh.antennapod.activity.OpmlImportFromPathActivity;
import de.danoeh.antennapod.asynctask.DocumentFileExportWorker;
import de.danoeh.antennapod.asynctask.ExportWorker;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.export.html.HtmlWriter;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.dialog.ChooseDataFolderDialog;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.util.List;

public class StoragePreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "StoragePrefFragment";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_OPML_IMPORT = "prefOpmlImport";
    private static final String PREF_HTML_EXPORT = "prefHtmlExport";
    private static final String IMPORT_EXPORT = "importExport";
    private static final String PREF_CHOOSE_DATA_DIR = "prefChooseDataDir";
    private static final String[] EXTERNAL_STORAGE_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    private static final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 41;
    private static final int CHOOSE_OPML_EXPORT_PATH = 1;
    private Disposable disposable;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_storage);
        setupStorageScreen();
    }

    @Override
    public void onResume() {
        super.onResume();
        setDataFolderText();
    }

    @Override
    public void onStop() {
        super.onStop();
        unsubscribeExportSubscription();
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
                    openExportPathPicker();
                    return true;
                }
        );
        findPreference(PREF_HTML_EXPORT).setOnPreferenceClickListener(
                preference -> export(new HtmlWriter()));
        findPreference(PREF_OPML_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    activity.startActivity(new Intent(activity, OpmlImportFromPathActivity.class));
                    return true;
                });
        findPreference(PREF_CHOOSE_DATA_DIR).setOnPreferenceClickListener(
                preference -> {
                    if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT &&
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        showChooseDataFolderDialog();
                    } else {
                        int readPermission = ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.READ_EXTERNAL_STORAGE);
                        int writePermission = ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (readPermission == PackageManager.PERMISSION_GRANTED &&
                                writePermission == PackageManager.PERMISSION_GRANTED) {
                            openDirectoryChooser();
                        } else {
                            requestPermission();
                        }
                    }
                    return true;
                }
        );
        findPreference(PREF_CHOOSE_DATA_DIR)
                .setOnPreferenceClickListener(
                        preference -> {
                            if (Build.VERSION.SDK_INT >= 19) {
                                showChooseDataFolderDialog();
                            } else {
                                Intent intent = new Intent(activity, DirectoryChooserActivity.class);
                                activity.startActivityForResult(intent,
                                        DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
                            }
                            return true;
                        }
                );
        findPreference(UserPreferences.PREF_IMAGE_CACHE_SIZE).setOnPreferenceChangeListener(
                (preference, o) -> {
                    if (o instanceof String) {
                        int newValue = Integer.parseInt((String) o) * 1024 * 1024;
                        if (newValue != UserPreferences.getImageCacheSize()) {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                            dialog.setTitle(android.R.string.dialog_alert_title);
                            dialog.setMessage(R.string.pref_restart_required);
                            dialog.setPositiveButton(android.R.string.ok, null);
                            dialog.show();
                        }
                        return true;
                    }
                    return false;
                }
        );
    }

    private boolean export(ExportWriter exportWriter) {
        return export(exportWriter, null);
    }

    private boolean export(ExportWriter exportWriter, final Uri uri) {
        Context context = getActivity();
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.exporting_label));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        if (uri == null) {
            Observable<File> observable = new ExportWorker(exportWriter).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                context.getString(R.string.provider_authority), output);
                        showExportSuccessDialog(context, context.getString(R.string.export_success_sum, output.toString()), fileUri);
                    }, error -> {
                        showExportErrorDialog(context, error);
                    }, progressDialog::dismiss);
        } else {
            Observable<DocumentFile> observable = new DocumentFileExportWorker(exportWriter, context, uri).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        showExportSuccessDialog(context, context.getString(R.string.export_success_sum, output.getUri()), output.getUri());
                    }, error -> {
                        showExportErrorDialog(context, error);
                    }, progressDialog::dismiss);
        }
        return true;
    }

    private void showExportSuccessDialog(final Context context, final String message, final Uri streamUri) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(context)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_success_title);
        alert.setMessage(message);
        alert.setPositiveButton(R.string.send_label, (dialog, which) -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                    context.getResources().getText(R.string.opml_export_label));
            sendIntent.putExtra(Intent.EXTRA_STREAM, streamUri);
            sendIntent.setType("text/plain");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, streamUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
            context.startActivity(Intent.createChooser(sendIntent,
                    context.getResources().getText(R.string.send_label)));
        });
        alert.create().show();
    }

    private void showExportErrorDialog(final Context context, final Throwable error) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(context)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_error_label);
        alert.setMessage(error.getMessage());
        alert.show();
    }

    public void unsubscribeExportSubscription() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @SuppressLint("NewApi")
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK &&
                requestCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
            String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);

            File path;
            if (dir != null) {
                path = new File(dir);
            } else {
                path = getActivity().getExternalFilesDir(null);
            }
            String message = null;
            final Context context = getActivity().getApplicationContext();
            if (!path.exists()) {
                message = String.format(context.getString(R.string.folder_does_not_exist_error), dir);
            } else if (!path.canRead()) {
                message = String.format(context.getString(R.string.folder_not_readable_error), dir);
            } else if (!path.canWrite()) {
                message = String.format(context.getString(R.string.folder_not_writable_error), dir);
            }

            if (message == null) {
                Log.d(TAG, "Setting data folder: " + dir);
                UserPreferences.setDataFolder(dir);
                setDataFolderText();
            } else {
                AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
                ab.setMessage(message);
                ab.setPositiveButton(android.R.string.ok, null);
                ab.show();
            }
        }

        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_OPML_EXPORT_PATH) {
            Uri uri = data.getData();
            export(new OpmlWriter(), uri);
        }
    }

    private void setDataFolderText() {
        File f = UserPreferences.getDataFolder(null);
        if (f != null) {
            findPreference(PREF_CHOOSE_DATA_DIR)
                    .setSummary(f.getAbsolutePath());
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(getActivity(), EXTERNAL_STORAGE_PERMISSIONS,
                PERMISSION_REQUEST_EXTERNAL_STORAGE);
    }

    private void openDirectoryChooser() {
        Activity activity = getActivity();
        Intent intent = new Intent(activity, DirectoryChooserActivity.class);
        activity.startActivityForResult(intent, DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED);
    }

    private void openExportPathPicker() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            Intent intentPickAction = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            // Creates an implicit intent to launch a file manager which lets
            // the user choose a specific directory to export to.
            try {
                startActivityForResult(intentPickAction, CHOOSE_OPML_EXPORT_PATH);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found. Should never happen...");
            }
        }

        // If we are using a SDK lower than API 21 or the implicit intent failed
        // fallback to the legacy export process
        export(new OpmlWriter());
    }

    private void showChooseDataFolderDialog() {
        ChooseDataFolderDialog.showDialog(
                getActivity(), new ChooseDataFolderDialog.RunnableWithString() {
                    @Override
                    public void run(final String folder) {
                        UserPreferences.setDataFolder(folder);
                        setDataFolderText();
                    }
                });
    }
}
