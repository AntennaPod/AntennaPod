package de.danoeh.antennapod.ui.preferences.screen.bugreport;

import android.app.Application;
import android.os.Build;
import android.text.format.DateUtils;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import de.danoeh.antennapod.system.CrashReportWriter;
import de.danoeh.antennapod.system.utils.PackageUtils;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Viewmodel encapsulating all data and business logic required
 * to present the report bug UI.
 */
public class BugReportViewModel extends AndroidViewModel {
    /**
     * Device runtime environment information
     */
    public static class EnvironmentInfo {
        String applicationVersion;
        String androidVersion;
        String androidOsVersion;
        String deviceManufacturer;
        String deviceModel;
        String deviceName;
        String productName;

        private EnvironmentInfo(Application application) {
            this.applicationVersion = PackageUtils.getApplicationVersion(application);
            this.androidVersion = Build.VERSION.RELEASE;
            this.androidOsVersion = System.getProperty("os.version");
            this.deviceManufacturer = Build.MANUFACTURER;
            this.deviceModel = Build.MODEL;
            this.deviceName = Build.DEVICE;
            this.productName = Build.PRODUCT;
        }

        public String getFriendlyDeviceName() {
            if (Build.MODEL.toLowerCase(Locale.getDefault()).startsWith(Build.MANUFACTURER
                    .toLowerCase(Locale.getDefault()))) {
                return Build.MODEL;
            }
            return Build.MANUFACTURER + " " + Build.MODEL;
        }
    }

    /**
     * Contents of the latest crash log / stacktrace file
     */
    public static class CrashLogInfo {
        private final Date timestamp;
        private final String content;

        private CrashLogInfo() {
            this.timestamp = CrashReportWriter.getTimestamp();
            this.content = CrashReportWriter.read();
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public String getContent() {
            return content;
        }

        public Boolean isAvailable() {
            return timestamp != null && !content.isEmpty();
        }
    }

    /**
     * Full UI state required by the report bug presentation layer
     */
    public static class UiState {
        public enum CrashLogState {
            UNAVAILABLE,
            SHOWN_COLLAPSED,
            SHOWN_EXPANDED
        }

        private final EnvironmentInfo environmentInfo;
        private final CrashLogInfo crashLogInfo;
        private CrashLogState crashLogState;

        private final String formattedEnvironmentInfo;
        private String formattedCrashLogTimestamp;
        private String formattedCrashLog;

        private UiState(Application application) {
            this.environmentInfo = new EnvironmentInfo(application);
            this.crashLogInfo = new CrashLogInfo();

            this.formattedEnvironmentInfo = "## Environment"
                    + "\nAndroid version: " + environmentInfo.androidVersion
                    + "\nOS version: " + environmentInfo.androidOsVersion
                    + "\nAntennaPod version: " + environmentInfo.applicationVersion
                    + "\nModel: " + environmentInfo.deviceModel
                    + "\nDevice: " + environmentInfo.deviceName
                    + "\nProduct: " + environmentInfo.productName
                    + "\nManufacturer: " + environmentInfo.deviceManufacturer;

            if (crashLogInfo.isAvailable()) {
                this.formattedCrashLogTimestamp = DateUtils.formatDateTime(
                        application, crashLogInfo.timestamp.getTime(),
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
                this.formattedCrashLog = "## Crash info"
                        + "\nTime: " + df.format(crashLogInfo.getTimestamp())
                        + "\nAntennaPod version: " + environmentInfo.applicationVersion
                        + "\n"
                        + "\nStackTrace"
                        + "\n```"
                        + "\n" + crashLogInfo.getContent()
                        + "\n```";
                this.crashLogState = CrashLogState.SHOWN_COLLAPSED;
            } else {
                this.crashLogState = CrashLogState.UNAVAILABLE;
            }
        }

        public EnvironmentInfo getEnvironmentInfo() {
            return this.environmentInfo;
        }

        public CrashLogInfo getCrashLogInfo() {
            return this.crashLogInfo;
        }

        public CrashLogState getCrashLogState() {
            return this.crashLogState;
        }

        public String getFormattedCrashLogTimestamp() {
            return this.formattedCrashLogTimestamp;
        }

        public String getBugReportWithMarkup() {
            if (crashLogInfo.isAvailable()) {
                return getEnvironmentInfoWithMarkup() + "\n\n" + getCrashInfoWithMarkup();
            }
            return formattedEnvironmentInfo;
        }

        public String getEnvironmentInfoWithMarkup() {
            return formattedEnvironmentInfo;
        }

        public String getCrashInfoWithMarkup() {
            return this.formattedCrashLog;
        }

        private void setCrashLogState(CrashLogState crashLogState) {
            this.crashLogState = crashLogState;
        }
    }

    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();
    private final Disposable disposable;

    public BugReportViewModel(Application application) {
        super(application);
        // Does file I/O, so we have to use a background thread
        this.disposable = Observable.fromCallable(() -> new UiState(application))
                .subscribeOn(Schedulers.io())
                .subscribe(this.uiState::postValue);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposable.dispose();
    }

    public LiveData<UiState> getState() {
        return uiState;
    }

    public UiState requireCurrentState() {
        return Objects.requireNonNull(uiState.getValue(), "UiState is NULL!");
    }

    public void setCrashLogState(UiState.CrashLogState crashLogState) {
        UiState currentUiState = uiState.getValue();

        if (currentUiState != null) {
            if (currentUiState.getCrashLogState() != crashLogState) {
                currentUiState.setCrashLogState(crashLogState);
                uiState.setValue(currentUiState);
            }
        }
    }
}
