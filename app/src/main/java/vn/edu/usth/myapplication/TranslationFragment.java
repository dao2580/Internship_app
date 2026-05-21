/*
 * Copyright (c) 2025 Android project OpenVision API
 * All rights reserved.
 * Project: My Application
 * File: TranslationFragment.java
 * Last Modified: 17/10/2025 0:56
 */

package vn.edu.usth.myapplication;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.usth.myapplication.data.AppRepository;

public class TranslationFragment extends Fragment {

    private static final String ARG_DETECTED_OBJECTS = "detected_objects";
    private static final String ARG_PHOTO_URI = "photo_uri";
    private static final String ARG_USER_INPUT_TEXT = "user_input_text";

    private final Map<String, String> languageMap = new HashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private ImageView imgPreview;
    private TextView txtObjectDetected;
    private TextView txtSourceLanguage;
    private TextView txtOfflineModelStatus;

    private TextInputEditText etSourceText;
    private TextInputEditText etTranslatedText;

    private AutoCompleteTextView spinnerTargetLanguage;

    private MaterialButton btnTranslate;
    private MaterialButton btnSpeak;
    private MaterialButton btnBack;
    private MaterialButton btnGoHome;

    private ProgressBar progressBar;

    private String[] detectedObjects;
    private String photoUri;
    private String userInputText;

    private AzureTranslatorService translatorService;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private String currentTargetCode = "vi";

    private OfflineTranslatorService offlineTranslatorService;
    private boolean isOfflineModelReady = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            detectedObjects = getArguments().getStringArray(ARG_DETECTED_OBJECTS);
            photoUri = getArguments().getString(ARG_PHOTO_URI);
            userInputText = getArguments().getString(ARG_USER_INPUT_TEXT);
        }

        translatorService = new AzureTranslatorService();

        tts = new TextToSpeech(getContext(), status -> {
            ttsReady = status == TextToSpeech.SUCCESS;

            if (ttsReady) {
                setTtsLanguage(currentTargetCode);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_translation, container, false);

        imgPreview = v.findViewById(R.id.img_preview_small);
        txtObjectDetected = v.findViewById(R.id.txt_object_detected);
        txtSourceLanguage = v.findViewById(R.id.txt_source_language);
        txtOfflineModelStatus = v.findViewById(R.id.txt_offline_model_status);

        etSourceText = v.findViewById(R.id.et_source_text);
        etTranslatedText = v.findViewById(R.id.et_translated_text);

        spinnerTargetLanguage = v.findViewById(R.id.spinner_target_language);

        btnTranslate = v.findViewById(R.id.btn_translate);
        btnSpeak = v.findViewById(R.id.btn_speak);
        btnGoHome = v.findViewById(R.id.btn_go_home);
        btnBack = v.findViewById(R.id.btn_back);

        progressBar = v.findViewById(R.id.progress_bar);

        setupLanguageDropdown();
        bindSimpleUi();

        return v;
    }

    private void bindSimpleUi() {
        if (imgPreview != null && photoUri != null && !photoUri.isEmpty()) {
            try {
                imgPreview.setImageURI(android.net.Uri.parse(photoUri));
            } catch (Exception ignored) {
            }
        }

        String first = detectedObjects != null && detectedObjects.length > 0
                ? detectedObjects[0]
                : null;

        if (first != null) {
            txtObjectDetected.setText(getString(R.string.object_detected, first));
            etSourceText.setText(first);
        } else if (userInputText != null && !userInputText.isEmpty()) {
            txtObjectDetected.setText(R.string.object_detected_none);
            etSourceText.setText(userInputText);
        } else {
            txtObjectDetected.setText(R.string.object_detected_none);
        }

        txtSourceLanguage.setText(R.string.source_language_english);
        updateOfflineModelStatus(getString(R.string.offline_preparing));

        btnBack.setOnClickListener(v ->
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigateUp()
        );

        btnTranslate.setOnClickListener(v -> translate());

        btnSpeak.setOnClickListener(v -> speak(1.0f));
        btnSpeak.setVisibility(View.GONE);

        btnGoHome.setOnClickListener(view -> {
            NavController navController =
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);

            boolean moved = navController.popBackStack(R.id.nav_home, false);

            if (!moved) {
                navController.navigate(R.id.nav_home);
            }
        });
    }

    private void setupLanguageDropdown() {
        languageMap.clear();
        languageNames.clear();

        boolean vietnameseUi = !"en".equalsIgnoreCase(
                SettingsPreferences.getAppLanguageCode(requireContext())
        );

        String[] displayNames = vietnameseUi
                ? SettingsPreferences.LANGUAGE_NAMES_VI
                : SettingsPreferences.LANGUAGE_NAMES_EN;

        for (int i = 0; i < SettingsPreferences.LANGUAGE_CODES.length; i++) {
            String name = displayNames[i];
            String code = SettingsPreferences.LANGUAGE_CODES[i];

            languageNames.add(name);
            languageMap.put(name, code);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );

        spinnerTargetLanguage.setAdapter(adapter);

        String defaultCode = SettingsPreferences.getDefaultLanguageCode(requireContext());
        String defaultName = SettingsPreferences.getLanguageNameFromCode(defaultCode, vietnameseUi);

        spinnerTargetLanguage.setText(defaultName, false);

        currentTargetCode = defaultCode;
        prepareOfflineTranslator(currentTargetCode);

        spinnerTargetLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = languageNames.get(position);
            String selectedCode = languageMap.get(selectedName);

            if (selectedCode == null) {
                selectedCode = "vi";
            }

            currentTargetCode = selectedCode;

            setTtsLanguage(currentTargetCode);
            prepareOfflineTranslator(currentTargetCode);
        });
    }

    private void prepareOfflineTranslator(String targetCode) {
        prepareOfflineTranslator(targetCode, false);
    }

    private void prepareOfflineTranslator(String targetCode, boolean isRetry) {
        try {
            if (offlineTranslatorService != null) {
                offlineTranslatorService.close();
            }
        } catch (Exception ignored) {
        }

        isOfflineModelReady = false;
        updateOfflineModelStatus(getString(R.string.offline_checking));

        offlineTranslatorService = new OfflineTranslatorService("en", targetCode);

        offlineTranslatorService.downloadModel(new OfflineTranslatorService.DownloadCallback() {
            @Override
            public void onSuccess() {
                isOfflineModelReady = true;
                updateOfflineModelStatus(getString(R.string.offline_ready));
            }

            @Override
            public void onFailure(Exception e) {
                if (!isRetry) {
                    updateOfflineModelStatus(getString(R.string.offline_retrying));
                    runOnUi(() -> prepareOfflineTranslator(targetCode, true));
                } else {
                    isOfflineModelReady = false;

                    if (NetworkUtils.isInternetAvailable(requireContext())) {
                        updateOfflineModelStatus(getString(R.string.offline_download_error));
                        toast(getString(R.string.model_download_failed));
                    } else {
                        updateOfflineModelStatus(getString(R.string.offline_no_model));
                        toast(getString(R.string.language_model_not_downloaded));
                    }

                    e.printStackTrace();
                }
            }
        });
    }

    private void updateOfflineModelStatus(String message) {
        runOnUi(() -> {
            if (txtOfflineModelStatus != null) {
                txtOfflineModelStatus.setText(message);
            }
        });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        if (btnTranslate != null) {
            btnTranslate.setEnabled(!loading);
        }
    }

    private int setTtsLanguage(String code) {
        if (!ttsReady || tts == null) {
            return TextToSpeech.ERROR;
        }

        try {
            Locale locale = Locale.forLanguageTag(code);
            int result = tts.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts.setLanguage(Locale.US);
            }

            return result;
        } catch (Exception e) {
            return TextToSpeech.ERROR;
        }
    }

    private void translate() {
        String src = safeText(etSourceText);

        if (src.isEmpty()) {
            toast(getString(R.string.enter_text_to_translate));
            return;
        }

        setTTSforCurrentTarget();
        setLoading(true);

        if (NetworkUtils.isInternetAvailable(requireContext())) {
            translatorService.translate(src, currentTargetCode, new AzureTranslatorService.TranslationCallback() {
                @Override
                public void onSuccess(String out) {
                    runOnUi(() -> {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        etTranslatedText.setText(out);
                        setLoading(false);
                        btnSpeak.setVisibility(View.VISIBLE);

                        if (!ttsReady) {
                            btnSpeak.setEnabled(false);
                            toast(getString(R.string.translation_completed_speech_unavailable));
                        } else {
                            btnSpeak.setEnabled(true);
                            toast(getString(R.string.translation_completed));
                        }

                        saveLearnedWord(out, "manual");
                    });
                }

                @Override
                public void onError(String err) {
                    runOnUi(() -> {
                        if (!isAdded() || getView() == null) {
                            return;
                        }

                        translateOffline(src);
                    });
                }
            });
        } else {
            translateOffline(src);
        }
    }

    private void translateOffline(String text) {
        if (!isAdded() || getView() == null) {
            return;
        }

        if (!isOfflineModelReady) {
            setLoading(false);
            updateOfflineModelStatus(getString(R.string.offline_not_ready));
            toast(getString(R.string.offline_model_not_ready));
            etTranslatedText.setText("");
            return;
        }

        offlineTranslatorService.translate(text, new OfflineTranslatorService.TranslationCallback() {
            @Override
            public void onSuccess(String translated) {
                runOnUi(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    etTranslatedText.setText(translated);
                    setLoading(false);
                    btnSpeak.setVisibility(View.VISIBLE);

                    if (!ttsReady) {
                        btnSpeak.setEnabled(false);
                        toast(getString(R.string.offline_translation_completed_speech_unavailable));
                    } else {
                        btnSpeak.setEnabled(true);
                        toast(getString(R.string.offline_translation_completed));
                    }

                    saveLearnedWord(translated, "manual");
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUi(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    setLoading(false);
                    toast(getString(R.string.offline_translation_failed));
                });
            }
        });
    }

    private void saveLearnedWord(String translatedText, String mode) {
        String userEmail = new UserDatabase(getContext()).getLoggedInEmail();

        if (userEmail == null) {
            return;
        }

        String sourceText = safeText(etSourceText);
        String vi = VocabMap.getVI(sourceText.toLowerCase());

        new AppRepository(getContext()).saveLearnedWord(
                userEmail,
                sourceText,
                vi,
                translatedText,
                currentTargetCode,
                mode
        );
    }

    private void setTTSforCurrentTarget() {
        setTtsLanguage(currentTargetCode);
    }

    private void speak(float speed) {
        if (!ttsReady || tts == null) {
            toast(getString(R.string.tts_unavailable));
            return;
        }

        String text = safeText(etTranslatedText);

        if (text.isEmpty()) {
            toast(getString(R.string.no_text_to_speak));
            return;
        }

        tts.stop();
        tts.setSpeechRate(speed);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS");
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void runOnUi(Runnable runnable) {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(runnable);
        }
    }

    @Override
    public void onDestroy() {
        if (offlineTranslatorService != null) {
            try {
                offlineTranslatorService.close();
            } catch (Exception ignored) {
            }
        }

        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }

        super.onDestroy();
    }
}