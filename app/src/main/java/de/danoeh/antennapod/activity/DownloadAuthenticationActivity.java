package de.danoeh.antennapod.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.feed.FeedMedia;
import de.danoeh.antennapod.core.feed.FeedPreferences;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.DownloadRequest;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.storage.DBWriter;
import de.danoeh.antennapod.core.storage.DownloadRequester;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import org.apache.commons.lang3.Validate;


/**
 * Shows a username and a password text field.
 * The activity MUST be started with the ARG_DOWNlOAD_REQUEST argument set to a non-null value.
 */
public class DownloadAuthenticationActivity extends AppCompatActivity {

    /**
     * The download request object that contains information about the resource that requires a username and a password.
     */
    public static final String ARG_DOWNLOAD_REQUEST = "request";

    private EditText etxtUsername;
    private EditText etxtPassword;
    private DownloadRequest request;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getNoTitleTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_authentication_activity);

        etxtUsername = findViewById(R.id.etxtUsername);
        etxtPassword = findViewById(R.id.etxtPassword);

        Validate.isTrue(getIntent().hasExtra(ARG_DOWNLOAD_REQUEST), "Download request missing");
        request = getIntent().getParcelableExtra(ARG_DOWNLOAD_REQUEST);

        TextView txtvDescription = findViewById(R.id.txtvDescription);
        String newDescription = txtvDescription.getText() + ":\n\n" + request.getTitle();
        txtvDescription.setText(newDescription);

        if (savedInstanceState != null) {
            etxtUsername.setText(savedInstanceState.getString("username"));
            etxtPassword.setText(savedInstanceState.getString("password"));
        }

        findViewById(R.id.butConfirm).setOnClickListener(v ->
                Completable.fromAction(this::updatePassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    DownloadRequester.getInstance().download(DownloadAuthenticationActivity.this, request);
                    finish();
                }));

        findViewById(R.id.butCancel).setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            finish();
        });

    }

    private void updatePassword() {
        String username = etxtUsername.getText().toString();
        String password = etxtPassword.getText().toString();
        request.setUsername(username);
        request.setPassword(password);

        if (request.getFeedfileType() == FeedMedia.FEEDFILETYPE_FEEDMEDIA) {
            long mediaId = request.getFeedfileId();
            FeedMedia media = DBReader.getFeedMedia(mediaId);
            if (media != null) {
                FeedPreferences preferences = media.getItem().getFeed().getPreferences();
                if (TextUtils.isEmpty(preferences.getPassword()) || TextUtils.isEmpty(preferences.getUsername())) {
                    preferences.setUsername(username);
                    preferences.setPassword(password);
                    DBWriter.setFeedPreferences(preferences);
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", etxtUsername.getText().toString());
        outState.putString("password", etxtPassword.getText().toString());
    }
}
