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

    private static final String[][] LANGS = {
            {"Arabic","ar"}, {"Chinese (Simplified)","zh-Hans"}, {"Chinese (Traditional)","zh-Hant"},
            {"Czech","cs"}, {"Danish","da"}, {"Dutch","nl"}, {"English","en"}, {"Filipino","fil"},
            {"Finnish","fi"}, {"French","fr"}, {"German","de"}, {"Greek","el"}, {"Hebrew","he"},
            {"Hindi","hi"}, {"Hungarian","hu"}, {"Indonesian","id"}, {"Italian","it"},
            {"Japanese","ja"}, {"Korean","ko"}, {"Malay","ms"}, {"Norwegian","no"},
            {"Polish","pl"}, {"Portuguese","pt"}, {"Romanian","ro"}, {"Russian","ru"},
            {"Spanish","es"}, {"Swedish","sv"}, {"Thai","th"}, {"Turkish","tr"}, {"Vietnamese","vi"}
    };

    private final Map<String, String> languageMap = new HashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private ImageView imgPreview;

    private TextView txtObjectDetected, txtSourceLanguage, txtOfflineModelStatus;
    private TextInputEditText etSourceText, etTranslatedText;
    private AutoCompleteTextView spinnerTargetLanguage;
    private MaterialButton btnTranslate, btnSpeak, btnBack, btnGoHome;
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

        for (String[] row : LANGS) {
            languageMap.put(row[0], row[1]);
            languageNames.add(row[0]);
        }
        languageNames.sort(String::compareTo);

        translatorService = new AzureTranslatorService();

        tts = new TextToSpeech(getContext(), status -> {
            ttsReady = (status == TextToSpeech.SUCCESS);
            if (ttsReady) {
                setTtsLanguage(currentTargetCode);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

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

        String first = (detectedObjects != null && detectedObjects.length > 0)
                ? detectedObjects[0]
                : null;

        if (first != null) {
            txtObjectDetected.setText("Object detected: " + first);
            etSourceText.setText(first);
        } else if (userInputText != null && !userInputText.isEmpty()) {
            txtObjectDetected.setText("Object detected: NONE");
            etSourceText.setText(userInputText);
        } else {
            txtObjectDetected.setText("Object detected: NONE");
        }

        txtSourceLanguage.setText("Source language: English");
        updateOfflineModelStatus("Offline: đang chuẩn bị...");

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
        updateOfflineModelStatus("Offline: đang kiểm tra...");

        offlineTranslatorService = new OfflineTranslatorService("en", targetCode);

        offlineTranslatorService.downloadModel(new OfflineTranslatorService.DownloadCallback() {
            @Override
            public void onSuccess() {
                isOfflineModelReady = true;
                updateOfflineModelStatus("Offline: đã sẵn sàng");
            }

            @Override
            public void onFailure(Exception e) {
                if (!isRetry) {
                    updateOfflineModelStatus("Offline: đang thử lại...");
                    runOnUi(() -> prepareOfflineTranslator(targetCode, true));
                } else {
                    isOfflineModelReady = false;

                    if (NetworkUtils.isInternetAvailable(requireContext())) {
                        updateOfflineModelStatus("Offline: tải lỗi");
                        toast("Tải model offline thất bại");
                    } else {
                        updateOfflineModelStatus("Offline: chưa có model");
                        toast("Ngôn ngữ này chưa được tải offline trước đó");
                    }

                    e.printStackTrace();
                }
            }
        });
    }

    private void setupLanguageDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );

        spinnerTargetLanguage.setAdapter(adapter);
        spinnerTargetLanguage.setText("Vietnamese", false);
        currentTargetCode = "vi";

        prepareOfflineTranslator(currentTargetCode);

        spinnerTargetLanguage.setOnItemClickListener((parent, view, position, id) -> {
            currentTargetCode = languageMap.get(languageNames.get(position));
            setTtsLanguage(currentTargetCode);
            prepareOfflineTranslator(currentTargetCode);
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
        if (!ttsReady || tts == null) return TextToSpeech.ERROR;

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
            toast("Please enter text to translate");
            return;
        }

        setTTSforCurrentTarget();
        setLoading(true);

        if (NetworkUtils.isInternetAvailable(requireContext())) {
            translatorService.translate(src, currentTargetCode,
                    new AzureTranslatorService.TranslationCallback() {
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
                                    toast("Translation completed! (Speech unavailable)");
                                } else {
                                    btnSpeak.setEnabled(true);
                                    toast("Translation completed!");
                                }

                                String userEmail = new UserDatabase(getContext()).getLoggedInEmail();
                                if (userEmail != null) {
                                    String sourceText = safeText(etSourceText);
                                    String vi = VocabMap.getVI(sourceText.toLowerCase());

                                    new AppRepository(getContext()).saveLearnedWord(
                                            userEmail,
                                            sourceText,
                                            vi,
                                            out,
                                            currentTargetCode,
                                            "manual"
                                    );
                                }
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
            updateOfflineModelStatus("Offline: chưa sẵn sàng");
            toast("Offline model chưa sẵn sàng");
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
                        toast("Offline translation completed! (Speech unavailable)");
                    } else {
                        btnSpeak.setEnabled(true);
                        toast("Offline translation completed!");
                    }

                    String userEmail = new UserDatabase(getContext()).getLoggedInEmail();
                    if (userEmail != null) {
                        String sourceText = safeText(etSourceText);
                        String vi = VocabMap.getVI(sourceText.toLowerCase());

                        new AppRepository(getContext()).saveLearnedWord(
                                userEmail,
                                sourceText,
                                vi,
                                translated,
                                currentTargetCode,
                                "manual"
                        );
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUi(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }

                    setLoading(false);
                    toast("Offline translation failed");
                });
            }
        });
    }

    private void setTTSforCurrentTarget() {
        setTtsLanguage(currentTargetCode);
    }

    private void speak(float speed) {
        if (!ttsReady || tts == null) {
            toast("Text-to-Speech unavailable");
            return;
        }

        String text = safeText(etTranslatedText);
        if (text.isEmpty()) {
            toast("No text to speak");
            return;
        }

        tts.stop();
        tts.setSpeechRate(speed);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS");
    }

    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void toast(String s) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        }
    }

    private void runOnUi(Runnable r) {
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(r);
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