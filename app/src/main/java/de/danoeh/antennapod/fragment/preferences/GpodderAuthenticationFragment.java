package de.danoeh.antennapod.fragment.preferences;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.GpodnetPreferences;
import de.danoeh.antennapod.core.service.download.AntennapodHttpClient;
import de.danoeh.antennapod.core.sync.SyncService;
import de.danoeh.antennapod.core.sync.gpoddernet.GpodnetService;
import de.danoeh.antennapod.core.sync.gpoddernet.model.GpodnetDevice;
import de.danoeh.antennapod.core.util.FileNameGenerator;
import de.danoeh.antennapod.core.util.IntentUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guides the user through the authentication process.
 */
public class GpodderAuthenticationFragment extends DialogFragment {
    public static final String TAG = "GpodnetAuthActivity";

    private ViewFlipper viewFlipper;

    private static final int STEP_DEFAULT = -1;
    private static final int STEP_HOSTNAME = 0;
    private static final int STEP_LOGIN = 1;
    private static final int STEP_DEVICE = 2;
    private static final int STEP_FINISH = 3;

    private int currentStep = -1;

    private GpodnetService service;
    private volatile String username;
    private volatile String password;
    private volatile GpodnetDevice selectedDevice;
    private List<GpodnetDevice> devices;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle(GpodnetService.DEFAULT_BASE_HOST);
        dialog.setNegativeButton(R.string.cancel_label, null);
        dialog.setCancelable(false);
        this.setCancelable(false);

        View root = View.inflate(getContext(), R.layout.gpodnetauth_dialog, null);
        viewFlipper = root.findViewById(R.id.viewflipper);
        advance();
        dialog.setView(root);

        return dialog.create();
    }

    private void setupHostView(View view) {
        final Button selectHost = view.findViewById(R.id.chooseHostButton);
        final RadioGroup serverRadioGroup = view.findViewById(R.id.serverRadioGroup);
        final EditText serverUrlText = view.findViewById(R.id.serverUrlText);
        if (!GpodnetService.DEFAULT_BASE_HOST.equals(GpodnetPreferences.getHostname())) {
            serverUrlText.setText(GpodnetPreferences.getHostname());
        }
        final TextInputLayout serverUrlTextInput = view.findViewById(R.id.serverUrlTextInput);
        serverRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            serverUrlTextInput.setVisibility(checkedId == R.id.customServerRadio ? View.VISIBLE : View.GONE);
        });
        selectHost.setOnClickListener(v -> {
            if (serverRadioGroup.getCheckedRadioButtonId() == R.id.customServerRadio) {
                GpodnetPreferences.setHostname(serverUrlText.getText().toString());
            } else {
                GpodnetPreferences.setHostname(GpodnetService.DEFAULT_BASE_HOST);
            }
            service = new GpodnetService(AntennapodHttpClient.getHttpClient(), GpodnetPreferences.getHostname());
            getDialog().setTitle(GpodnetPreferences.getHostname());
            advance();
        });
    }

    private void setupLoginView(View view) {
        final EditText username = view.findViewById(R.id.etxtUsername);
        final EditText password = view.findViewById(R.id.etxtPassword);
        final Button login = view.findViewById(R.id.butLogin);
        final TextView txtvError = view.findViewById(R.id.credentialsError);
        final ProgressBar progressBar = view.findViewById(R.id.progBarLogin);
        final TextView createAccount = view.findViewById(R.id.createAccountButton);

        createAccount.setPaintFlags(createAccount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        createAccount.setOnClickListener(v -> IntentUtils.openInBrowser(getContext(), "https://gpodder.net/register/"));

        password.setOnEditorActionListener((v, actionID, event) ->
                actionID == EditorInfo.IME_ACTION_GO && login.performClick());

        login.setOnClickListener(v -> {
            final String usernameStr = username.getText().toString();
            final String passwordStr = password.getText().toString();

            if (usernameHasUnwantedChars(usernameStr)) {
                txtvError.setText(R.string.gpodnetsync_username_characters_error);
                txtvError.setVisibility(View.VISIBLE);
                return;
            }

            login.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            txtvError.setVisibility(View.GONE);
            InputMethodManager inputManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(login.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

            Completable.fromAction(() -> {
                service.authenticate(usernameStr, passwordStr);
                devices = service.getDevices();
                GpodderAuthenticationFragment.this.username = usernameStr;
                GpodderAuthenticationFragment.this.password = passwordStr;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(() -> {
                        login.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                        advance();
                    }, error -> {
                            login.setEnabled(true);
                            progressBar.setVisibility(View.GONE);
                            txtvError.setText(error.getCause().getMessage());
                            txtvError.setVisibility(View.VISIBLE);
                        });

        });
    }

    private void setupDeviceView(View view) {
        final EditText deviceName = view.findViewById(R.id.deviceName);
        final LinearLayout devicesContainer = view.findViewById(R.id.devicesContainer);
        deviceName.setText(generateDeviceName());

        MaterialButton createDeviceButton = view.findViewById(R.id.createDeviceButton);
        createDeviceButton.setOnClickListener(v -> createDevice(view));

        for (GpodnetDevice device : devices) {
            View row = View.inflate(getContext(), R.layout.gpodnetauth_device_row, null);
            Button selectDeviceButton = row.findViewById(R.id.selectDeviceButton);
            selectDeviceButton.setOnClickListener(v -> {
                selectedDevice = device;
                advance();
            });
            selectDeviceButton.setText(device.getCaption());
            devicesContainer.addView(row);
        }
    }

    private void createDevice(View view) {
        final EditText deviceName = view.findViewById(R.id.deviceName);
        final TextView txtvError = view.findViewById(R.id.deviceSelectError);
        final ProgressBar progBarCreateDevice = view.findViewById(R.id.progbarCreateDevice);

        String deviceNameStr = deviceName.getText().toString();
        if (isDeviceInList(deviceNameStr)) {
            return;
        }
        progBarCreateDevice.setVisibility(View.VISIBLE);
        txtvError.setVisibility(View.GONE);
        deviceName.setEnabled(false);

        Observable.fromCallable(() -> {
            String deviceId = generateDeviceId(deviceNameStr);
            service.configureDevice(deviceId, deviceNameStr, GpodnetDevice.DeviceType.MOBILE);
            return new GpodnetDevice(deviceId, deviceNameStr, GpodnetDevice.DeviceType.MOBILE.toString(), 0);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(device -> {
                    progBarCreateDevice.setVisibility(View.GONE);
                    selectedDevice = device;
                    advance();
                }, error -> {
                        deviceName.setEnabled(true);
                        progBarCreateDevice.setVisibility(View.GONE);
                        txtvError.setText(error.getMessage());
                        txtvError.setVisibility(View.VISIBLE);
                    });
    }

    private String generateDeviceName() {
        String baseName = getString(R.string.gpodnetauth_device_name_default, Build.MODEL);
        String name = baseName;
        int num = 1;
        while (isDeviceInList(name)) {
            name = baseName + " (" + num + ")";
            num++;
        }
        return name;
    }

    private String generateDeviceId(String name) {
        // devices names must be of a certain form:
        // https://gpoddernet.readthedocs.org/en/latest/api/reference/general.html#devices
        return FileNameGenerator.generateFileName(name).replaceAll("\\W", "_").toLowerCase(Locale.US);
    }

    private boolean isDeviceInList(String name) {
        if (devices == null) {
            return false;
        }
        String id = generateDeviceId(name);
        for (GpodnetDevice device : devices) {
            if (device.getId().equals(id) || device.getCaption().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private GpodnetDevice findDevice(String id) {
        if (devices == null) {
            return null;
        }
        for (GpodnetDevice device : devices) {
            if (device.getId().equals(id)) {
                return device;
            }
        }
        return null;
    }

    private void setupFinishView(View view) {
        final Button sync = view.findViewById(R.id.butSyncNow);

        sync.setOnClickListener(v -> {
            dismiss();
            SyncService.sync(getContext());
        });
    }

    private void writeLoginCredentials() {
        GpodnetPreferences.setUsername(username);
        GpodnetPreferences.setPassword(password);
        GpodnetPreferences.setDeviceID(selectedDevice.getId());
    }

    private void advance() {
        if (currentStep < STEP_FINISH) {

            View view = viewFlipper.getChildAt(currentStep + 1);
            if (currentStep == STEP_DEFAULT) {
                setupHostView(view);
            } else if (currentStep == STEP_HOSTNAME) {
                setupLoginView(view);
            } else if (currentStep == STEP_LOGIN) {
                if (username == null || password == null) {
                    throw new IllegalStateException("Username and password must not be null here");
                } else {
                    setupDeviceView(view);
                }
            } else if (currentStep == STEP_DEVICE) {
                if (selectedDevice == null) {
                    throw new IllegalStateException("Device must not be null here");
                } else {
                    writeLoginCredentials();
                    setupFinishView(view);
                }
            }
            if (currentStep != STEP_DEFAULT) {
                viewFlipper.showNext();
            }
            currentStep++;
        } else {
            dismiss();
        }
    }

    private boolean usernameHasUnwantedChars(String username) {
        Pattern special = Pattern.compile("[!@#$%&*()+=|<>?{}\\[\\]~]");
        Matcher containsUnwantedChars = special.matcher(username);
        return containsUnwantedChars.find();
    }
}
