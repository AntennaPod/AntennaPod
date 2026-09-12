package de.danoeh.antennapod.ui.preferences.screen;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;
import de.danoeh.antennapod.event.ModelDownloadEvent;
import de.danoeh.antennapod.net.ai.service.ad.vosk.VoskTranscriptionManager;
import de.danoeh.antennapod.net.ai.service.ad.vosk.VoskModel;
import de.danoeh.antennapod.storage.preferences.AzureOpenAiPreferences;
import de.danoeh.antennapod.storage.preferences.CloudAiPreferences;
import de.danoeh.antennapod.storage.preferences.LocalAiPreferences;
import de.danoeh.antennapod.storage.preferences.OpenAiPreferences;
import de.danoeh.antennapod.ui.preferences.R;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AiPreferencesFragment extends AnimatedPreferenceFragment {
    private static final String PREF_CLOUD_AI_PROVIDER = "prefCloudAiProvider";
    private static final String PREF_OPENAI_API_KEY = "prefOpenAiApiKey";
    private static final String PREF_OPENAI_ANALYSIS_MODEL = "prefOpenAiAnalysisModel";
    private static final String PREF_OPENAI_TRANSCRIPTION_MODEL = "prefOpenAiTranscriptionModel";

    // Azure OpenAI
    private static final String PREF_AZURE_ENDPOINT = "prefAzureEndpoint";
    private static final String PREF_AZURE_API_KEY = "prefAzureApiKey";
    private static final String PREF_AZURE_CHAT_DEPLOYMENT = "prefAzureChatDeployment";
    private static final String PREF_AZURE_WHISPER_DEPLOYMENT = "prefAzureWhisperDeployment";
    private static final String PREF_AZURE_API_VERSION = "prefAzureApiVersion";

    // Local Transcription
    private static final String PREF_LOCAL_TRANSCRIPTION_ENABLED = "prefLocalTranscriptionEnabled";
    private static final String PREF_LOCAL_TRANSCRIPTION_MODEL = "prefLocalTranscriptionModel";
    private static final String PREF_MANAGE_TRANSCRIPTION_MODELS = "prefManageTranscriptionModels";
    private static final String PREF_DELETE_ALL_TRANSCRIPTION_MODELS = "prefDeleteAllTranscriptionModels";

    private VoskTranscriptionManager transcriptionManager;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences_ai);

        transcriptionManager = new VoskTranscriptionManager(requireContext());

        setupCloudProviderPreference();
        setupApiKeyPreference();
        setupOpenAiModelPreferences();
        setupAzurePreferences();
        setupLocalTranscriptionPreferences();
        setupDeleteAllTranscriptionModels();
        updateCloudProviderVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLocalTranscriptionUI();
        updateDeleteAllTranscriptionModelsSummary();
    }

    @Override
    public void onStart() {
        super.onStart();
        requireActivity().setTitle(R.string.pref_ai_label);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onModelDownloadEvent(ModelDownloadEvent event) {
        // Update UI when a model download completes
        if (event.getStatus() == ModelDownloadEvent.Status.COMPLETED) {
            updateLocalTranscriptionUI();
            updateDeleteAllTranscriptionModelsSummary();
        }
    }

    private void setupCloudProviderPreference() {
        ListPreference providerPref = findPreference(PREF_CLOUD_AI_PROVIDER);
        if (providerPref == null) {
            return;
        }
        providerPref.setValue(CloudAiPreferences.getProvider(requireContext()));
        providerPref.setOnPreferenceChangeListener((preference, newValue) -> {
            CloudAiPreferences.setProvider(requireContext(), (String) newValue);
            providerPref.setValue((String) newValue);
            updateCloudProviderVisibility();
            return false;
        });
    }

    private void updateCloudProviderVisibility() {
        boolean azure = CloudAiPreferences.isAzure(requireContext());
        setPreferenceVisible(PREF_OPENAI_API_KEY, !azure);
        setPreferenceVisible(PREF_OPENAI_ANALYSIS_MODEL, !azure);
        setPreferenceVisible(PREF_OPENAI_TRANSCRIPTION_MODEL, !azure);
        setPreferenceVisible(PREF_AZURE_ENDPOINT, azure);
        setPreferenceVisible(PREF_AZURE_API_KEY, azure);
        setPreferenceVisible(PREF_AZURE_CHAT_DEPLOYMENT, azure);
        setPreferenceVisible(PREF_AZURE_WHISPER_DEPLOYMENT, azure);
        setPreferenceVisible(PREF_AZURE_API_VERSION, azure);
    }

    private void setPreferenceVisible(String key, boolean visible) {
        Preference preference = findPreference(key);
        if (preference != null) {
            preference.setVisible(visible);
        }
    }

    private void setupAzurePreferences() {
        setupTextPreference(PREF_AZURE_ENDPOINT,
                () -> AzureOpenAiPreferences.getEndpoint(requireContext()),
                value -> AzureOpenAiPreferences.setEndpoint(requireContext(), value),
                R.string.pref_azure_endpoint_summary,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        setupAzureApiKeyPreference();
        setupTextPreference(PREF_AZURE_CHAT_DEPLOYMENT,
                () -> AzureOpenAiPreferences.getChatDeployment(requireContext()),
                value -> AzureOpenAiPreferences.setChatDeployment(requireContext(), value),
                R.string.pref_azure_chat_deployment_summary,
                InputType.TYPE_CLASS_TEXT);
        setupTextPreference(PREF_AZURE_WHISPER_DEPLOYMENT,
                () -> AzureOpenAiPreferences.getTranscriptionDeployment(requireContext()),
                value -> AzureOpenAiPreferences.setTranscriptionDeployment(requireContext(), value),
                R.string.pref_azure_whisper_deployment_summary,
                InputType.TYPE_CLASS_TEXT);
        setupTextPreference(PREF_AZURE_API_VERSION,
                () -> AzureOpenAiPreferences.getApiVersion(requireContext()),
                value -> AzureOpenAiPreferences.setApiVersion(requireContext(), value),
                R.string.pref_azure_api_version_summary,
                InputType.TYPE_CLASS_TEXT);
    }

    private interface ValueReader {
        String get();
    }

    private interface ValueWriter {
        void set(String value);
    }

    private void setupTextPreference(String prefKey, ValueReader reader, ValueWriter writer,
            int emptySummaryRes, int inputType) {
        EditTextPreference pref = findPreference(prefKey);
        if (pref == null) {
            return;
        }
        pref.setOnBindEditTextListener(editText -> {
            editText.setInputType(inputType);
            editText.setText(reader.get());
        });
        pref.setOnPreferenceChangeListener((preference, newValue) -> {
            writer.set((String) newValue);
            updateTextSummary(pref, reader.get(), emptySummaryRes);
            pref.setText("");
            return false; // Stored in encrypted preferences, not default shared preferences
        });
        updateTextSummary(pref, reader.get(), emptySummaryRes);
    }

    private void updateTextSummary(EditTextPreference pref, String value, int emptySummaryRes) {
        if (TextUtils.isEmpty(value)) {
            pref.setSummary(emptySummaryRes);
        } else {
            pref.setSummary(value);
        }
    }

    private void setupAzureApiKeyPreference() {
        EditTextPreference apiKeyPref = findPreference(PREF_AZURE_API_KEY);
        if (apiKeyPref == null) {
            return;
        }
        apiKeyPref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setText(AzureOpenAiPreferences.getApiKey(requireContext()));
        });
        apiKeyPref.setOnPreferenceChangeListener((preference, newValue) -> {
            AzureOpenAiPreferences.setApiKey(requireContext(), (String) newValue);
            updateAzureApiKeySummary(apiKeyPref);
            apiKeyPref.setText("");
            return false; // Avoid storing in default shared preferences
        });
        updateAzureApiKeySummary(apiKeyPref);
    }

    private void updateAzureApiKeySummary(EditTextPreference apiKeyPref) {
        String key = AzureOpenAiPreferences.getApiKey(requireContext());
        if (TextUtils.isEmpty(key)) {
            apiKeyPref.setSummary(R.string.pref_azure_api_key_summary);
        } else {
            apiKeyPref.setSummary(R.string.pref_openai_api_key_set);
        }
    }

    private void setupApiKeyPreference() {
        EditTextPreference apiKeyPref = findPreference(PREF_OPENAI_API_KEY);
        if (apiKeyPref == null) {
            return;
        }
        apiKeyPref.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setText(OpenAiPreferences.getApiKey(requireContext()));
        });
        apiKeyPref.setOnPreferenceChangeListener((preference, newValue) -> {
            OpenAiPreferences.setApiKey(requireContext(), (String) newValue);
            updateApiKeySummary(apiKeyPref);
            apiKeyPref.setText("");
            return false; // Avoid storing in default shared preferences
        });
        updateApiKeySummary(apiKeyPref);
    }

    private void setupOpenAiModelPreferences() {
        setupTextPreference(PREF_OPENAI_ANALYSIS_MODEL,
                () -> OpenAiPreferences.getAnalysisModel(requireContext()),
                value -> OpenAiPreferences.setAnalysisModel(requireContext(), value),
                R.string.pref_openai_analysis_model_summary,
                InputType.TYPE_CLASS_TEXT);
        setupTextPreference(PREF_OPENAI_TRANSCRIPTION_MODEL,
                () -> OpenAiPreferences.getTranscriptionModel(requireContext()),
                value -> OpenAiPreferences.setTranscriptionModel(requireContext(), value),
                R.string.pref_openai_transcription_model_summary,
                InputType.TYPE_CLASS_TEXT);
    }

    private void setupLocalTranscriptionPreferences() {
        // Model Selection
        setupTranscriptionModelList();

        // Manage Models
        setupManageTranscriptionModels();

        // Enable Switch
        SwitchPreferenceCompat transcriptionSwitch = findPreference(PREF_LOCAL_TRANSCRIPTION_ENABLED);
        if (transcriptionSwitch != null) {
            transcriptionSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = (Boolean) newValue;
                LocalAiPreferences.setLocalTranscriptionEnabled(requireContext(), enabled);
                updateLocalTranscriptionUI();
                return true;
            });
        }
    }

    private void setupManageTranscriptionModels() {
        Preference managePref = findPreference(PREF_MANAGE_TRANSCRIPTION_MODELS);
        if (managePref != null) {
            managePref.setOnPreferenceClickListener(preference -> {
                ((de.danoeh.antennapod.ui.preferences.PreferenceController) requireActivity())
                        .openScreen(new TranscriptionModelManagerFragment());
                return true;
            });
        }
    }

    private void setupTranscriptionModelList() {
        ListPreference modelPref = findPreference(PREF_LOCAL_TRANSCRIPTION_MODEL);
        if (modelPref != null) {
            modelPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String modelId = (String) newValue;

                // Check if model is downloaded
                if (!transcriptionManager.isModelDownloaded(modelId)) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Model not downloaded")
                            .setMessage(
                                    "The selected model is not downloaded. "
                                            + "Please download it in 'Manage Models' first.")
                            .setPositiveButton("Go to Manage Models", (d, w) -> {
                                ((de.danoeh.antennapod.ui.preferences.PreferenceController) requireActivity())
                                        .openScreen(new TranscriptionModelManagerFragment());
                            })
                            .setNegativeButton(android.R.string.cancel, null)
                            .show();
                }

                LocalAiPreferences.setLocalTranscriptionModel(requireContext(), modelId);
                updateLocalTranscriptionUI();
                return true;
            });
        }
    }

    private void updateLocalTranscriptionUI() {
        String selectedModel = LocalAiPreferences.getLocalTranscriptionModel(requireContext());
        boolean isDownloaded = transcriptionManager.isModelDownloaded(selectedModel);

        ListPreference modelPref = findPreference(PREF_LOCAL_TRANSCRIPTION_MODEL);
        if (modelPref != null) {
            // Update summary
            de.danoeh.antennapod.net.ai.service.ad.vosk.VoskModel model = transcriptionManager
                    .getModelById(selectedModel);
            String label = (model != null) ? model.getName() : selectedModel;
            modelPref.setSummary(label);
            modelPref.setValue(selectedModel); // Ensure UI matches pref

            List<VoskModel> models = transcriptionManager
                    .getAvailableModels();
            // Filter only downloaded models
            List<VoskModel> downloadedModels = new java.util.ArrayList<>();
            for (VoskModel m : models) {
                if (transcriptionManager.isModelDownloaded(m.getId())) {
                    downloadedModels.add(m);
                }
            }

            CharSequence[] entries = new CharSequence[downloadedModels.size()];
            CharSequence[] entryValues = new CharSequence[downloadedModels.size()];
            for (int i = 0; i < downloadedModels.size(); i++) {
                de.danoeh.antennapod.net.ai.service.ad.vosk.VoskModel m = downloadedModels.get(i);
                entries[i] = m.getName();
                entryValues[i] = m.getId();
            }
            modelPref.setEntries(entries);
            modelPref.setEntryValues(entryValues);
        }

        // Update enable switch
        SwitchPreferenceCompat enabledPref = findPreference(PREF_LOCAL_TRANSCRIPTION_ENABLED);
        if (enabledPref != null) {
            enabledPref.setEnabled(isDownloaded);
            if (!isDownloaded) {
                // Model was deleted - disable the feature and uncheck the toggle
                if (enabledPref.isChecked()) {
                    enabledPref.setChecked(false);
                    LocalAiPreferences.setLocalTranscriptionEnabled(requireContext(), false);
                }
                enabledPref.setSummary("Model not downloaded");
            } else {
                enabledPref.setSummary(R.string.pref_local_transcription_summary);
            }
        }
    }

    private void setupDeleteAllTranscriptionModels() {
        Preference deleteAllPref = findPreference(PREF_DELETE_ALL_TRANSCRIPTION_MODELS);
        if (deleteAllPref != null) {
            deleteAllPref.setOnPreferenceClickListener(preference -> {
                showDeleteAllTranscriptionModelsConfirmation();
                return true;
            });
        }
        updateDeleteAllTranscriptionModelsSummary();
    }

    private void updateDeleteAllTranscriptionModelsSummary() {
        Preference deleteAllPref = findPreference(PREF_DELETE_ALL_TRANSCRIPTION_MODELS);
        if (deleteAllPref == null) {
            return;
        }

        int count = transcriptionManager.getDownloadedModelsCount();
        long size = transcriptionManager.getDownloadedModelsSize();

        if (count == 0) {
            deleteAllPref.setSummary(R.string.pref_delete_all_transcription_models_summary);
        } else {
            String sizeStr = formatSize(size);
            String summary = count + " model" + (count > 1 ? "s" : "") + " (" + sizeStr + ")";
            deleteAllPref.setSummary(summary);
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void showDeleteAllTranscriptionModelsConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_delete_all_transcription_models_confirm_title)
                .setMessage(R.string.pref_delete_all_transcription_models_confirm_message)
                .setPositiveButton(R.string.confirm_label, (dialog, which) -> {
                    deleteAllTranscriptionModels();
                })
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private void deleteAllTranscriptionModels() {
        // Disable local features if enabled
        if (LocalAiPreferences.isLocalTranscriptionEnabled(requireContext())) {
            LocalAiPreferences.setLocalTranscriptionEnabled(requireContext(), false);
            SwitchPreferenceCompat transcriptionSwitch = findPreference(PREF_LOCAL_TRANSCRIPTION_ENABLED);
            if (transcriptionSwitch != null) {
                transcriptionSwitch.setChecked(false);
            }
        }

        String keepTranscription = LocalAiPreferences.getLocalTranscriptionModel(requireContext());
        int totalCount = transcriptionManager.deleteAllModelsExcept(keepTranscription);

        // Update UI
        updateLocalTranscriptionUI();
        updateDeleteAllTranscriptionModelsSummary();

        // Show result
        if (totalCount > 0) {
            Toast.makeText(requireContext(),
                    getString(R.string.pref_delete_all_transcription_models_success, totalCount),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(),
                    R.string.pref_delete_all_transcription_models_none,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateApiKeySummary(EditTextPreference apiKeyPref) {
        String key = OpenAiPreferences.getApiKey(requireContext());
        if (TextUtils.isEmpty(key)) {
            apiKeyPref.setSummary(R.string.pref_openai_api_key_summary);
        } else {
            apiKeyPref.setSummary(R.string.pref_openai_api_key_set);
        }
    }

}
