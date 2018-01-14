package de.danoeh.antennapod.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.service.download.ProxyConfig;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ProxyDialog {

    private static final String TAG = "ProxyDialog";

    private final Context context;

    private MaterialDialog dialog;

    private Spinner spType;
    private EditText etHost;
    private EditText etPort;
    private EditText etUsername;
    private EditText etPassword;

    private boolean testSuccessful = false;
    private TextView txtvMessage;
    private Subscription subscription;

    public ProxyDialog(Context context) {
        this.context = context;
    }

    public Dialog createDialog() {
        dialog = new MaterialDialog.Builder(context)
                .title(R.string.pref_proxy_title)
                .customView(R.layout.proxy_settings, true)
                .positiveText(R.string.proxy_test_label)
                .negativeText(R.string.cancel_label)
                .onPositive((dialog1, which) -> {
                    if(!testSuccessful) {
                        dialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                        test();
                        return;
                    }
                    String type = (String) ((Spinner) dialog1.findViewById(R.id.spType)).getSelectedItem();
                    ProxyConfig proxy;
                    if(Proxy.Type.valueOf(type) == Proxy.Type.DIRECT) {
                        proxy = ProxyConfig.direct();
                    } else {
                        String host = etHost.getText().toString();
                        String port = etPort.getText().toString();
                        String username = etUsername.getText().toString();
                        if(TextUtils.isEmpty(username)) {
                            username = null;
                        }
                        String password = etPassword.getText().toString();
                        if(TextUtils.isEmpty(password)) {
                            password = null;
                        }
                        int portValue = 0;
                        if(!TextUtils.isEmpty(port)) {
                            portValue = Integer.valueOf(port);
                        }
                        proxy = ProxyConfig.http(host, portValue, username, password);
                    }
                    UserPreferences.setProxyConfig(proxy);
                    AntennapodHttpClient.reinit();
                    dialog.dismiss();
                })
                .onNegative((dialog1, which) -> dialog1.dismiss())
                .autoDismiss(false)
                .build();
        View view = dialog.getCustomView();
        spType = (Spinner) view.findViewById(R.id.spType);
        String[] types = { Proxy.Type.DIRECT.name(), Proxy.Type.HTTP.name() };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
            android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spType.setAdapter(adapter);
        ProxyConfig proxyConfig = UserPreferences.getProxyConfig();
        spType.setSelection(adapter.getPosition(proxyConfig.type.name()));
        etHost = (EditText) view.findViewById(R.id.etHost);
        if(!TextUtils.isEmpty(proxyConfig.host)) {
            etHost.setText(proxyConfig.host);
        }
        etHost.addTextChangedListener(requireTestOnChange);
        etPort = (EditText) view.findViewById(R.id.etPort);
        if(proxyConfig.port > 0) {
            etPort.setText(String.valueOf(proxyConfig.port));
        }
        etPort.addTextChangedListener(requireTestOnChange);
        etUsername = (EditText) view.findViewById(R.id.etUsername);
        if(!TextUtils.isEmpty(proxyConfig.username)) {
            etUsername.setText(proxyConfig.username);
        }
        etUsername.addTextChangedListener(requireTestOnChange);
        etPassword = (EditText) view.findViewById(R.id.etPassword);
        if(!TextUtils.isEmpty(proxyConfig.password)) {
            etPassword.setText(proxyConfig.username);
        }
        etPassword.addTextChangedListener(requireTestOnChange);
        if(proxyConfig.type == Proxy.Type.DIRECT) {
            enableSettings(false);
            setTestRequired(false);
        }
        spType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                enableSettings(position > 0);
                setTestRequired(position > 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                enableSettings(false);
            }
        });
        txtvMessage = (TextView) view.findViewById(R.id.txtvMessage);
        checkValidity();
        return dialog;
    }

    private final TextWatcher requireTestOnChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            setTestRequired(true);
        }
    };

    private void enableSettings(boolean enable) {
        etHost.setEnabled(enable);
        etPort.setEnabled(enable);
        etUsername.setEnabled(enable);
        etPassword.setEnabled(enable);
    }

    private boolean checkValidity() {
        boolean valid = true;
        if(spType.getSelectedItemPosition() > 0) {
            valid &= checkHost();
        }
        valid &= checkPort();
        return valid;
    }

    private boolean checkHost() {
        String host = etHost.getText().toString();
        if(host.length() == 0) {
            etHost.setError(context.getString(R.string.proxy_host_empty_error));
            return false;
        }
        if(!"localhost".equals(host) && !Patterns.DOMAIN_NAME.matcher(host).matches()) {
            etHost.setError(context.getString(R.string.proxy_host_invalid_error));
            return false;
        }
        return true;
    }

    private boolean checkPort() {
        int port = getPort();
        if(port < 0 && port > 65535) {
            etPort.setError(context.getString(R.string.proxy_port_invalid_error));
            return false;
        }
        return true;
    }

    private int getPort() {
        String port = etPort.getText().toString();
        if(port.length() > 0) {
            try {
                return Integer.parseInt(port);
            } catch(NumberFormatException e) {
                // ignore
            }
        }
        return 0;
    }

    private void setTestRequired(boolean required) {
        if(required) {
            testSuccessful = false;
            MDButton button = dialog.getActionButton(DialogAction.POSITIVE);
            button.setText(context.getText(R.string.proxy_test_label));
            button.setEnabled(true);
        } else {
            testSuccessful = true;
            MDButton button = dialog.getActionButton(DialogAction.POSITIVE);
            button.setText(context.getText(android.R.string.ok));
            button.setEnabled(true);
        }
    }

    private void test() {
        if(subscription != null) {
            subscription.unsubscribe();
        }
        if(!checkValidity()) {
            setTestRequired(true);
            return;
        }
        TypedArray res = context.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary });
        int textColorPrimary = res.getColor(0, 0);
        res.recycle();
        String checking = context.getString(R.string.proxy_checking);
        txtvMessage.setTextColor(textColorPrimary);
        txtvMessage.setText("{fa-circle-o-notch spin} " + checking);
        txtvMessage.setVisibility(View.VISIBLE);
        subscription = Observable.create((Observable.OnSubscribe<Response>) subscriber -> {
            String type = (String) spType.getSelectedItem();
            String host = etHost.getText().toString();
            String port = etPort.getText().toString();
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            int portValue = 8080;
            if(!TextUtils.isEmpty(port)) {
                portValue = Integer.valueOf(port);
            }
            SocketAddress address = InetSocketAddress.createUnresolved(host, portValue);
            Proxy.Type proxyType = Proxy.Type.valueOf(type.toUpperCase());
            Proxy proxy = new Proxy(proxyType, address);
            OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .proxy(proxy);
            builder.interceptors().clear();
            OkHttpClient client = builder.build();
            if(!TextUtils.isEmpty(username)) {
                String credentials = Credentials.basic(username, password);
                client.interceptors().add(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("Proxy-Authorization", credentials).build();
                    return chain.proceed(request);
                });
            }
            Request request = new Request.Builder()
                    .url("http://www.google.com")
                    .head()
                    .build();
            try {
                Response response = client.newCall(request).execute();
                subscriber.onNext(response);
            } catch(IOException e) {
                subscriber.onError(e);
            }
            subscriber.onCompleted();
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        response -> {
                            int colorId;
                            String icon;
                            String result;
                            if(response.isSuccessful()) {
                                colorId = R.color.download_success_green;
                                icon = "{fa-check}";
                                result = context.getString(R.string.proxy_test_successful);
                            } else {
                                colorId = R.color.download_failed_red;
                                icon = "{fa-close}";
                                result = context.getString(R.string.proxy_test_failed);
                            }
                            int color = ContextCompat.getColor(context, colorId);
                            txtvMessage.setTextColor(color);
                            String message = String.format("%s %s: %s", icon, result, response.message());
                            txtvMessage.setText(message);
                            setTestRequired(!response.isSuccessful());
                        },
                        error -> {
                            String icon = "{fa-close}";
                            String result = context.getString(R.string.proxy_test_failed);
                            int color = ContextCompat.getColor(context, R.color.download_failed_red);
                            txtvMessage.setTextColor(color);
                            String message = String.format("%s %s: %s", icon, result, error.getMessage());
                            txtvMessage.setText(message);
                            setTestRequired(true);
                        }
                );
    }

}
