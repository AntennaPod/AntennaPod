package de.danoeh.antennapod.activity;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.asynctask.ExportWorker;
import de.danoeh.antennapod.core.export.ExportWriter;
import de.danoeh.antennapod.core.export.opml.OpmlWriter;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.util.IntentUtils;
import de.danoeh.antennapod.core.util.StorageUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Lets the user select the path for the OPML-export process
 */
public class OpmlExportToPathActivity extends OpmlImportBaseActivity {

    private static final String TAG = "OpmlImportFromPathAct";

    private static final int CHOOSE_OPML_EXPORT_PATH = 1;

    private Intent intentPickAction;
    private Intent intentGetContentAction;

    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.opml_export);

        final TextView txtvHeaderExplanation1 = findViewById(R.id.txtvHeadingExplanation1);
        final TextView txtvExplanation1 = findViewById(R.id.txtvExplanation1);
        final TextView txtvHeaderExplanation2 = findViewById(R.id.txtvHeadingExplanation2);
        final TextView txtvExplanation2 = findViewById(R.id.txtvExplanation2);

        Button butChooseFilesystem = findViewById(R.id.butChooseFileFromFilesystem);
        butChooseFilesystem.setOnClickListener(v -> chooseFileFromFilesystem());

        Button butChooseExternal = findViewById(R.id.butChooseFileFromExternal);
        butChooseExternal.setOnClickListener(v -> chooseFileFromExternal());

        int nextOption = 1;
        String optionLabel = getString(R.string.opml_import_option);
        intentPickAction = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

        if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
            intentPickAction.setData(null);
            if(!IntentUtils.isCallable(getApplicationContext(), intentPickAction)) {
                txtvHeaderExplanation1.setVisibility(View.GONE);
                txtvExplanation1.setVisibility(View.GONE);
                findViewById(R.id.divider1).setVisibility(View.GONE);
                butChooseFilesystem.setVisibility(View.GONE);
            }
        }
        if(txtvExplanation1.getVisibility() == View.VISIBLE) {
            txtvHeaderExplanation1.setText(String.format(optionLabel, nextOption));
            nextOption++;
        }

        intentGetContentAction = new Intent(Intent.ACTION_GET_CONTENT);
        intentGetContentAction.addCategory(Intent.CATEGORY_OPENABLE);
        intentGetContentAction.setType("*/*");
        if(!IntentUtils.isCallable(getApplicationContext(), intentGetContentAction)) {
            txtvHeaderExplanation2.setVisibility(View.GONE);
            txtvExplanation2.setVisibility(View.GONE);
            findViewById(R.id.divider2).setVisibility(View.GONE);
            butChooseExternal.setVisibility(View.GONE);
        } else {
            txtvHeaderExplanation2.setText(String.format(optionLabel, nextOption));
            nextOption++;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StorageUtils.checkStorageAvailability(this);
    }

    @Override
    public void onStop() {
        unsubscribeExportSubscription();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return false;
        }
    }

    /*
     * Creates an implicit intent to launch a file manager which lets
     * the user choose a specific OPML-file to import from.
     */
    private void chooseFileFromFilesystem() {
        try {
            startActivityForResult(intentPickAction, CHOOSE_OPML_EXPORT_PATH);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    private void chooseFileFromExternal() {
        try {
            startActivityForResult(intentGetContentAction, CHOOSE_OPML_EXPORT_PATH);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No activity found. Should never happen...");
        }
    }

    /**
      * Gets the path of the file chosen with chooseFileToImport()
      */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == CHOOSE_OPML_EXPORT_PATH) {
            Uri uri = data.getData();
            if(uri != null && uri.toString().startsWith("/")) {
                uri = Uri.parse("file://" + uri.toString());
            }
            export(new OpmlWriter(), uri);
        }
    }

    private boolean export(ExportWriter exportWriter, final Uri uri) {
        Context context = this;
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(context.getString(R.string.exporting_label));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        final AlertDialog.Builder alert = new AlertDialog.Builder(context)
                .setNeutralButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        Observable<File> observable = new ExportWorker(exportWriter, context, uri).exportObservable();
        disposable = observable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(output -> {
                    alert.setTitle(R.string.export_success_title);
                    String message = context.getString(R.string.export_success_sum, output.toString());
                    alert.setMessage(message);
                    alert.setPositiveButton(R.string.send_label, (dialog, which) -> {
                        Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext(),
                                context.getString(R.string.provider_authority), output);
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                                context.getResources().getText(R.string.opml_export_label));
                        sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        sendIntent.setType("text/plain");
                        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                            List<ResolveInfo> resInfoList = context.getPackageManager().queryIntentActivities(sendIntent, PackageManager.MATCH_DEFAULT_ONLY);
                            for (ResolveInfo resolveInfo : resInfoList) {
                                String packageName = resolveInfo.activityInfo.packageName;
                                context.grantUriPermission(packageName, fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            }
                        }
                        context.startActivity(Intent.createChooser(sendIntent,
                                context.getResources().getText(R.string.send_label)));
                    });
                    alert.create().show();
                }, error -> {
                    alert.setTitle(R.string.export_error_label);
                    alert.setMessage(error.getMessage());
                    alert.show();
                }, progressDialog::dismiss);
        return true;
    }

    private void unsubscribeExportSubscription() {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
