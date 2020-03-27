package de.danoeh.antennapod.fragment.preferences;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.OpmlImportActivity;
import de.danoeh.antennapod.activity.PreferenceActivity;
import de.danoeh.antennapod.activity.SplashActivity;
import de.danoeh.antennapod.asynctask.DocumentFileExportWorker;
import de.danoeh.antennapod.asynctask.ExportWorker;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.export.html.HtmlWriter;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import de.danoeh.antennapod.core.storage.DatabaseExporter;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ImportExportPreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "ImportExPrefFragment";
    private static final String PREF_OPML_EXPORT = "prefOpmlExport";
    private static final String PREF_OPML_IMPORT = "prefOpmlImport";
    private static final String PREF_HTML_EXPORT = "prefHtmlExport";
    private static final String PREF_DATABASE_IMPORT = "prefDatabaseImport";
    private static final String PREF_DATABASE_EXPORT = "prefDatabaseExport";
    private static final String DEFAULT_OPML_OUTPUT_NAME = "antennapod-feeds.opml";
    private static final String CONTENT_TYPE_OPML = "text/x-opml";
    private static final String DEFAULT_HTML_OUTPUT_NAME = "antennapod-feeds.html";
    private static final String CONTENT_TYPE_HTML = "text/html";
    private static final int REQUEST_CODE_CHOOSE_OPML_EXPORT_PATH = 1;
    private static final int REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH = 2;
    private static final int REQUEST_CODE_CHOOSE_HTML_EXPORT_PATH = 3;
    private static final int REQUEST_CODE_RESTORE_DATABASE = 4;
    private static final int REQUEST_CODE_BACKUP_DATABASE = 5;
    private static final String DATABASE_EXPORT_FILENAME = "AntennaPodBackup.db";
    private Disposable disposable;
    private ProgressDialog progressDialog;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_import_export);
        setupStorageScreen();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getContext().getString(R.string.please_wait));
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
        findPreference(PREF_OPML_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    openExportPathPicker(CONTENT_TYPE_OPML, DEFAULT_OPML_OUTPUT_NAME,
                            REQUEST_CODE_CHOOSE_OPML_EXPORT_PATH, new OpmlWriter());
                    return true;
                }
        );
        findPreference(PREF_HTML_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    openExportPathPicker(CONTENT_TYPE_HTML, DEFAULT_HTML_OUTPUT_NAME,
                            REQUEST_CODE_CHOOSE_HTML_EXPORT_PATH, new HtmlWriter());
                    return true;
                });
        findPreference(PREF_OPML_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    try {
                        Intent intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
                        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
                        intentGetContentAction.setType("*/*");
                        startActivityForResult(intentGetContentAction, REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "No activity found. Should never happen...");
                    }
                    return true;
                });
        findPreference(PREF_DATABASE_IMPORT).setOnPreferenceClickListener(
                preference -> {
                    importDatabase();
                    return true;
                });
        findPreference(PREF_DATABASE_EXPORT).setOnPreferenceClickListener(
                preference -> {
                    exportDatabase();
                    return true;
                });
    }

    private void exportWithWriter(ExportWriter exportWriter, final Uri uri) {
        Context context = getActivity();
        progressDialog.show();
        if (uri == null) {
            Observable<File> observable = new ExportWorker(exportWriter, getContext()).exportObservable();
            disposable = observable.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                context.getString(R.string.provider_authority), output);
                        showExportSuccessDialog(output.toString(), fileUri);
                    }, this::showExportErrorDialog, progressDialog::dismiss);
        } else {
            DocumentFileExportWorker worker = new DocumentFileExportWorker(exportWriter, context, uri);
            disposable = worker.exportObservable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(output ->
                            showExportSuccessDialog(output.getUri().toString(), output.getUri()),
                            this::showExportErrorDialog, progressDialog::dismiss);
        }
    }

    private void exportDatabase() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/x-sqlite3")
                    .putExtra(Intent.EXTRA_TITLE, DATABASE_EXPORT_FILENAME);

            startActivityForResult(intent, REQUEST_CODE_BACKUP_DATABASE);
        } else {
            File sd = Environment.getExternalStorageDirectory();
            File backupDB = new File(sd, DATABASE_EXPORT_FILENAME);
            progressDialog.show();
            disposable = Completable.fromAction(() ->
                        DatabaseExporter.exportToStream(new FileOutputStream(backupDB), getContext()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Snackbar.make(getView(), R.string.export_success_title, Snackbar.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }, this::showExportErrorDialog);
        }
    }

    private void importDatabase() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_RESTORE_DATABASE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.import_select_file)), REQUEST_CODE_RESTORE_DATABASE);
        }
    }

    private void showDatabaseImportSuccessDialog() {
        AlertDialog.Builder d = new AlertDialog.Builder(getContext());
        d.setMessage(R.string.import_ok);
        d.setCancelable(false);
        d.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            Intent intent = new Intent(getContext(), SplashActivity.class);
            ComponentName cn = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(cn);
            startActivity(mainIntent);
            Runtime.getRuntime().exit(0);
        });
        d.show();
    }

    private void showExportSuccessDialog(final String path, final Uri streamUri) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_success_title);
        alert.setMessage(getContext().getString(R.string.export_success_sum, path));
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
        progressDialog.dismiss();
        final AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
        alert.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        alert.setTitle(R.string.export_error_label);
        alert.setMessage(error.getMessage());
        alert.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();

        if (requestCode == REQUEST_CODE_CHOOSE_OPML_EXPORT_PATH) {
            exportWithWriter(new OpmlWriter(), uri);
        } else if (requestCode == REQUEST_CODE_CHOOSE_HTML_EXPORT_PATH) {
            exportWithWriter(new HtmlWriter(), uri);
        } else if (requestCode == REQUEST_CODE_RESTORE_DATABASE) {
            progressDialog.show();
            disposable = Completable.fromAction(() -> DatabaseExporter.importBackup(uri, getContext()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        showDatabaseImportSuccessDialog();
                        progressDialog.dismiss();
                    }, this::showExportErrorDialog);
        } else if (requestCode == REQUEST_CODE_BACKUP_DATABASE) {
            progressDialog.show();
            disposable = Completable.fromAction(() -> DatabaseExporter.exportToDocument(uri, getContext()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        Snackbar.make(getView(), R.string.export_success_title, Snackbar.LENGTH_LONG).show();
                        progressDialog.dismiss();
                    }, this::showExportErrorDialog);
        } else if (requestCode == REQUEST_CODE_CHOOSE_OPML_IMPORT_PATH) {
            Intent intent = new Intent(getContext(), OpmlImportActivity.class);
            intent.setData(uri);
            startActivity(intent);
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
        exportWithWriter(writer, null);
    }
}
