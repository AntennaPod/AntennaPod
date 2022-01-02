package de.danoeh.antennapod.net.sync.nextcloud;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import de.danoeh.antennapod.net.sync.HostnameParser;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public class NextcloudLoginFlow {
    private static final String TAG = "NextcloudLoginFlow";

    private final OkHttpClient httpClient;
    private final HostnameParser hostname;
    private final Context context;
    private final AuthenticationCallback callback;
    private String token;
    private String endpoint;
    private Disposable startDisposable;
    private Disposable pollDisposable;

    public NextcloudLoginFlow(OkHttpClient httpClient, String hostUrl, Context context,
                              AuthenticationCallback callback) {
        this.httpClient = httpClient;
        this.hostname = new HostnameParser(hostUrl);
        this.context = context;
        this.callback = callback;
    }

    public void start() {
        startDisposable = Observable.fromCallable(() -> {
            URL url = new URI(hostname.scheme, null, hostname.host, hostname.port,
                    "/index.php/login/v2", null, null).toURL();
            JSONObject result = doRequest(url, "");
            String loginUrl = result.getString("login");
            this.token = result.getJSONObject("poll").getString("token");
            this.endpoint = result.getJSONObject("poll").getString("endpoint");
            return loginUrl;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result));
                        context.startActivity(browserIntent);
                        poll();
                    }, error -> {
                        Log.e(TAG, Log.getStackTraceString(error));
                        callback.onNextcloudAuthError(error.getLocalizedMessage());
                    });
    }

    private void poll() {
        pollDisposable = Observable.fromCallable(() -> doRequest(URI.create(endpoint).toURL(), "token=" + token))
                .delay(1, TimeUnit.SECONDS)
                .retry(60 * 10) // 10 minutes
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    callback.onNextcloudAuthenticated(result.getString("server"),
                            result.getString("loginName"), result.getString("appPassword"));
                }, Throwable::printStackTrace);
    }

    public void cancel() {
        if (startDisposable != null) {
            startDisposable.dispose();
        }
        if (pollDisposable != null) {
            pollDisposable.dispose();
        }
    }

    private JSONObject doRequest(URL url, String bodyContent) throws IOException, JSONException {
        RequestBody requestBody = RequestBody.create(
                MediaType.get("application/x-www-form-urlencoded"), bodyContent);
        Request request = new Request.Builder().url(url).method("POST", requestBody).build();
        Response response = httpClient.newCall(request).execute();
        if (response.code() != 200) {
            throw new IOException("Return code " + response.code());
        }
        ResponseBody body = response.body();
        return new JSONObject(body.string());
    }

    public interface AuthenticationCallback {
        void onNextcloudAuthenticated(String server, String username, String password);

        void onNextcloudAuthError(String errorMessage);
    }
}
