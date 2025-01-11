package de.danoeh.antennapod.ui.screen.preferences;

import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.net.common.AntennapodHttpClient;
import de.danoeh.antennapod.model.download.ProxyConfig;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.preferences.databinding.ProxySettingsBinding;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProxyDialog {
    private final Context context;
    private AlertDialog dialog;
    private boolean testSuccessful = false;
    private Disposable disposable;
    private ProxySettingsBinding viewBinding;

    public ProxyDialog(Context context) {
        this.context = context;
    }

    public Dialog show() {
        viewBinding = ProxySettingsBinding.bind(View.inflate(context, R.layout.proxy_settings, null));

        dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.pref_proxy_title)
                .setView(viewBinding.getRoot())
                .setNegativeButton(R.string.cancel_label, null)
                .setPositiveButton(R.string.proxy_test_label, null)
                .setNeutralButton(R.string.reset, null)
                .show();
        // To prevent cancelling the dialog on button click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((view) -> {
            if (!testSuccessful) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                test();
                return;
            }
            setProxyConfig();
            AntennapodHttpClient.reinit();
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener((view) -> {
            viewBinding.hostText.getText().clear();
            viewBinding.portText.getText().clear();
            viewBinding.usernameText.getText().clear();
            viewBinding.passwordText.getText().clear();
            setProxyConfig();
        });

        ProxyConfig proxyConfig = UserPreferences.getProxyConfig();

        viewBinding.hostText.setText(proxyConfig.host);
        viewBinding.hostText.addTextChangedListener(requireTestOnChange);
        viewBinding.portText.setText(proxyConfig.port > 0 ? String.valueOf(proxyConfig.port) : "");
        viewBinding.portText.addTextChangedListener(requireTestOnChange);
        viewBinding.usernameText.setText(proxyConfig.username);
        viewBinding.usernameText.addTextChangedListener(requireTestOnChange);
        viewBinding.passwordText.setText(proxyConfig.password);
        viewBinding.passwordText.addTextChangedListener(requireTestOnChange);
        if (proxyConfig.type == Proxy.Type.DIRECT) {
            enableSettings(false);
            setTestRequired(false);
        }

        List<String> types = new ArrayList<>();
        types.add(Proxy.Type.DIRECT.name());
        types.add(Proxy.Type.HTTP.name());
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            types.add(Proxy.Type.SOCKS.name());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, types);
        viewBinding.proxyTypeSpinner.setAdapter(adapter);
        viewBinding.proxyTypeSpinner.setText(proxyConfig.type.name());
        viewBinding.proxyTypeSpinner.setOnClickListener(view -> {
            if (viewBinding.proxyTypeSpinner.getText().length() != 0) {
                viewBinding.proxyTypeSpinner.setText("");
                viewBinding.proxyTypeSpinner.postDelayed(viewBinding.proxyTypeSpinner::showDropDown, 100);
            }
        });
        viewBinding.proxyTypeSpinner.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                boolean isDirect = Proxy.Type.DIRECT.name().equals(viewBinding.proxyTypeSpinner.getText().toString());
                if (isDirect) {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
                } else {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.VISIBLE);
                }
                enableSettings(!isDirect);
                setTestRequired(!isDirect);
            }
        });
        checkValidity();
        return dialog;
    }

    private void setProxyConfig() {
        final String type = viewBinding.proxyTypeSpinner.getText().toString();
        final Proxy.Type typeEnum = Proxy.Type.valueOf(type);
        final String host = viewBinding.hostText.getText().toString();
        final String port = viewBinding.portText.getText().toString();

        String username = viewBinding.usernameText.getText().toString();
        if (TextUtils.isEmpty(username)) {
            username = null;
        }
        String password = viewBinding.passwordText.getText().toString();
        if (TextUtils.isEmpty(password)) {
            password = null;
        }
        int portValue = 0;
        if (!TextUtils.isEmpty(port)) {
            portValue = Integer.parseInt(port);
        }
        ProxyConfig config = new ProxyConfig(typeEnum, host, portValue, username, password);
        UserPreferences.setProxyConfig(config);
        AntennapodHttpClient.setProxyConfig(config);
    }

    private final TextWatcher requireTestOnChange = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            setTestRequired(true);
        }
    };

    private void enableSettings(boolean enable) {
        viewBinding.hostText.setEnabled(enable);
        viewBinding.portText.setEnabled(enable);
        viewBinding.usernameText.setEnabled(enable);
        viewBinding.passwordText.setEnabled(enable);
    }

    private boolean checkValidity() {
        boolean valid = true;
        if (!Proxy.Type.DIRECT.name().equals(viewBinding.proxyTypeSpinner.getText().toString())) {
            valid = checkHost();
        }
        valid &= checkPort();
        return valid;
    }

    private boolean checkHost() {
        String host = viewBinding.hostText.getText().toString();
        if (host.isEmpty()) {
            viewBinding.hostText.setError(context.getString(R.string.proxy_host_empty_error));
            return false;
        }
        if (!"localhost".equals(host) && !Patterns.DOMAIN_NAME.matcher(host).matches()) {
            viewBinding.hostText.setError(context.getString(R.string.proxy_host_invalid_error));
            return false;
        }
        return true;
    }

    private boolean checkPort() {
        int port = getPort();
        if (port < 0 || port > 65535) {
            viewBinding.portText.setError(context.getString(R.string.proxy_port_invalid_error));
            return false;
        }
        return true;
    }

    private int getPort() {
        String port = viewBinding.portText.getText().toString();
        if (!port.isEmpty()) {
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 0;
    }

    private void setTestRequired(boolean required) {
        if (required) {
            testSuccessful = false;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.proxy_test_label);
        } else {
            testSuccessful = true;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(android.R.string.ok);
        }
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
    }

    private void test() {
        if (disposable != null) {
            disposable.dispose();
        }
        if (!checkValidity()) {
            setTestRequired(true);
            return;
        }
        TypedArray res = context.getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorPrimary });
        int textColorPrimary = res.getColor(0, 0);
        res.recycle();
        viewBinding.infoLabel.setTextColor(textColorPrimary);
        viewBinding.infoLabel.setText(R.string.proxy_checking);
        viewBinding.infoLabel.setVisibility(View.VISIBLE);
        disposable = Completable.create(emitter -> {
            String type = viewBinding.proxyTypeSpinner.getText().toString();
            String host = viewBinding.hostText.getText().toString();
            String port = viewBinding.portText.getText().toString();
            String username = viewBinding.usernameText.getText().toString();
            String password = viewBinding.passwordText.getText().toString();
            int portValue = 8080;
            if (!TextUtils.isEmpty(port)) {
                portValue = Integer.parseInt(port);
            }
            SocketAddress address = InetSocketAddress.createUnresolved(host, portValue);
            Proxy.Type proxyType = Proxy.Type.valueOf(type.toUpperCase(Locale.US));
            OkHttpClient.Builder builder = AntennapodHttpClient.newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .proxy(new Proxy(proxyType, address));
            if (!TextUtils.isEmpty(username)) {
                builder.proxyAuthenticator((route, response) -> {
                    String credentials = Credentials.basic(username, password);
                    return response.request().newBuilder()
                            .header("Proxy-Authorization", credentials)
                            .build();
                });
            }
            OkHttpClient client = builder.build();
            Request request = new Request.Builder().url("https://www.example.com").head().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    emitter.onComplete();
                } else {
                    emitter.onError(new IOException(response.message()));
                }
            } catch (IOException e) {
                emitter.onError(e);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            viewBinding.infoLabel.setTextColor(
                                    ThemeUtils.getColorFromAttr(context, R.attr.icon_green));
                            viewBinding.infoLabel.setText(R.string.proxy_test_successful);
                            setTestRequired(false);
                        },
                        error -> {
                            error.printStackTrace();
                            viewBinding.infoLabel.setTextColor(
                                    ThemeUtils.getColorFromAttr(context, R.attr.icon_red));
                            String message = String.format("%s: %s",
                                    context.getString(R.string.proxy_test_failed), error.getMessage());
                            viewBinding.infoLabel.setText(message);
                            setTestRequired(true);
                        }
                );
    }

}
