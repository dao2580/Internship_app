package vn.edu.usth.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class PronunciationHelper {

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextToSpeech textToSpeech;
    private boolean isReady = false;
    private boolean isDestroyed = false;

    public PronunciationHelper(Context context) {
        this.context = context.getApplicationContext();

        textToSpeech = new TextToSpeech(this.context, status -> {
            mainHandler.postDelayed(() -> {
                if (isDestroyed || textToSpeech == null) {
                    return;
                }

                if (status == TextToSpeech.SUCCESS) {
                    isReady = true;
                    textToSpeech.setSpeechRate(0.85f);
                    textToSpeech.setPitch(1.0f);
                } else {
                    isReady = false;
                }
            }, 80);
        });
    }

    public void speak(String text) {
        speak(text, "en");
    }

    public void speak(String text, String languageCode) {
        if (isDestroyed) {
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            Toast.makeText(context, R.string.no_text_to_speak, Toast.LENGTH_SHORT).show();
            return;
        }

        if (textToSpeech == null || !isReady) {
            Toast.makeText(context, R.string.tts_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanText = text.trim();
        String cleanLanguageCode = normalizeLanguageCode(languageCode);

        try {
            Locale locale = Locale.forLanguageTag(cleanLanguageCode);
            int result = textToSpeech.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(context, R.string.tts_language_not_supported, Toast.LENGTH_SHORT).show();
                return;
            }

            textToSpeech.speak(
                    cleanText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "word_" + System.currentTimeMillis()
            );

        } catch (Exception e) {
            Toast.makeText(context, R.string.tts_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeLanguageCode(String languageCode) {
        if (languageCode == null || languageCode.trim().isEmpty()) {
            return "en";
        }

        String code = languageCode.trim();

        if (code.equalsIgnoreCase("zh")) {
            return "zh-CN";
        }

        if (code.equalsIgnoreCase("zh-Hans")) {
            return "zh-CN";
        }

        if (code.equalsIgnoreCase("zh-Hant")) {
            return "zh-TW";
        }

        return code;
    }

    public void destroy() {
        isDestroyed = true;
        isReady = false;
        mainHandler.removeCallbacksAndMessages(null);

        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
            } catch (Exception ignored) {
            }

            textToSpeech = null;
        }
    }
}