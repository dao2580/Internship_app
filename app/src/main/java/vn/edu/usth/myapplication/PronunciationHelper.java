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
            /*
             * TextToSpeech init callback có thể chạy rất sớm hoặc chạy sau khi Fragment đã destroy.
             * Vì vậy không dùng trực tiếp textToSpeech ngay trong callback.
             */
            mainHandler.postDelayed(() -> {
                if (isDestroyed || textToSpeech == null) {
                    return;
                }

                if (status == TextToSpeech.SUCCESS) {
                    try {
                        int result = textToSpeech.setLanguage(Locale.US);
                        textToSpeech.setSpeechRate(0.85f);
                        textToSpeech.setPitch(1.0f);

                        isReady = result != TextToSpeech.LANG_MISSING_DATA
                                && result != TextToSpeech.LANG_NOT_SUPPORTED;
                    } catch (Exception e) {
                        isReady = false;
                    }
                } else {
                    isReady = false;
                }
            }, 80);
        });
    }

    public void speak(String word) {
        if (isDestroyed) {
            return;
        }

        if (word == null || word.trim().isEmpty()) {
            Toast.makeText(context, R.string.no_text_to_speak, Toast.LENGTH_SHORT).show();
            return;
        }

        if (textToSpeech == null || !isReady) {
            Toast.makeText(context, R.string.tts_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            textToSpeech.speak(
                    word.trim(),
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "word_" + System.currentTimeMillis()
            );
        } catch (Exception e) {
            Toast.makeText(context, R.string.tts_unavailable, Toast.LENGTH_SHORT).show();
        }
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