package vn.edu.usth.myapplication;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class OfflineTranslatorService {

    public interface DownloadCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(Exception e);
    }

    private final Translator translator;

    public OfflineTranslatorService(String sourceLanguage, String targetLanguage) {
        TranslatorOptions options =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage)
                        .build();

        translator = Translation.getClient(options);
    }

    public void downloadModel(DownloadCallback callback) {
        DownloadConditions conditions = new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public void translate(String text, TranslationCallback callback) {
        translator.translate(text)
                .addOnSuccessListener(translatedText -> {
                    if (callback != null) {
                        callback.onSuccess(translatedText);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public void close() {
        translator.close();
    }
}