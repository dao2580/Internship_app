package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPreferences {

    private static final String PREFS_NAME = "PhotoMagicPrefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_DEFAULT_LANGUAGE_CODE = "default_language_code";

    public static final String[] LANGUAGE_NAMES = {
            "Vietnamese",
            "English",
            "Chinese",
            "Japanese",
            "Korean",
            "French",
            "German",
            "Spanish",
            "Thai",
            "Russian"
    };

    public static final String[] LANGUAGE_CODES = {
            "vi",
            "en",
            "zh",
            "ja",
            "ko",
            "fr",
            "de",
            "es",
            "th",
            "ru"
    };

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isDarkMode(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void setDarkMode(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, value).commit();
    }

    public static String getDefaultLanguageCode(Context context) {
        return prefs(context).getString(KEY_DEFAULT_LANGUAGE_CODE, "vi");
    }

    public static void setDefaultLanguageCode(Context context, String code) {
        prefs(context).edit().putString(KEY_DEFAULT_LANGUAGE_CODE, code).apply();
    }

    public static String getLanguageNameFromCode(String code) {
        if (code == null) return "Vietnamese";

        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equalsIgnoreCase(code)) {
                return LANGUAGE_NAMES[i];
            }
        }
        return "Vietnamese";
    }

    public static int getLanguageIndexFromCode(String code) {
        if (code == null) return 0;

        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equalsIgnoreCase(code)) {
                return i;
            }
        }
        return 0;
    }

    public static String getLanguageCodeAt(int index) {
        if (index < 0 || index >= LANGUAGE_CODES.length) return "vi";
        return LANGUAGE_CODES[index];
    }

    public static String getDefaultLanguageName(Context context) {
        return getLanguageNameFromCode(getDefaultLanguageCode(context));
    }
}