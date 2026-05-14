package vn.edu.usth.myapplication;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class PronunciationHelper {

    private final Context context;
    private TextToSpeech textToSpeech;
    private boolean isReady = false;

    public PronunciationHelper(Context context) {
        this.context = context.getApplicationContext();

        textToSpeech = new TextToSpeech(this.context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                textToSpeech.setSpeechRate(0.85f);
                textToSpeech.setPitch(1.0f);

                isReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
    }

    public void speak(String word) {
        if (word == null || word.trim().isEmpty()) {
            Toast.makeText(context, "Không có từ để phát âm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (textToSpeech == null || !isReady) {
            Toast.makeText(context, "Loa phát âm chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        textToSpeech.speak(
                word.trim(),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "word_" + System.currentTimeMillis()
        );
    }

    public void destroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}