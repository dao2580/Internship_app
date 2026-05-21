package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPreferences {

    private static final String PREFS_NAME = "PhotoMagicPrefs";

    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_DEFAULT_LANGUAGE_CODE = "default_language_code";
    private static final String KEY_APP_LANGUAGE_CODE = "app_language_code";

    public static final String[] LANGUAGE_NAMES_VI = {
            "Tiếng Việt",
            "Tiếng Anh",
            "Tiếng Trung",
            "Tiếng Nhật",
            "Tiếng Hàn",
            "Tiếng Pháp",
            "Tiếng Đức",
            "Tiếng Tây Ban Nha",
            "Tiếng Thái",
            "Tiếng Nga"
    };

    public static final String[] LANGUAGE_NAMES_EN = {
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

    /*
     * Giữ biến LANGUAGE_NAMES để các file cũ như TranslationFragment
     * nếu còn dùng SettingsPreferences.LANGUAGE_NAMES thì không bị lỗi build.
     */
    public static final String[] LANGUAGE_NAMES = LANGUAGE_NAMES_VI;

    public static final String[] LANGUAGE_CODES = {
            "vi", "en", "zh", "ja", "ko", "fr", "de", "es", "th", "ru"
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

    /*
     * Ngôn ngữ dịch mặc định:
     * dùng cho màn Translation, không dùng để đổi UI toàn app.
     */
    public static String getDefaultLanguageCode(Context context) {
        return prefs(context).getString(KEY_DEFAULT_LANGUAGE_CODE, "vi");
    }

    public static void setDefaultLanguageCode(Context context, String code) {
        prefs(context).edit().putString(KEY_DEFAULT_LANGUAGE_CODE, code).apply();
    }

    /*
     * Ngôn ngữ ứng dụng:
     * dùng để đổi giao diện toàn app.
     */
    public static String getAppLanguageCode(Context context) {
        return prefs(context).getString(KEY_APP_LANGUAGE_CODE, "vi");
    }

    public static void setAppLanguageCode(Context context, String code) {
        prefs(context).edit().putString(KEY_APP_LANGUAGE_CODE, code).commit();
    }

    public static String getLanguageNameFromCode(String code) {
        return getLanguageNameFromCode(code, true);
    }

    public static String getLanguageNameFromCode(String code, boolean vietnameseName) {
        if (code == null) {
            return vietnameseName ? "Tiếng Việt" : "Vietnamese";
        }

        String[] names = vietnameseName ? LANGUAGE_NAMES_VI : LANGUAGE_NAMES_EN;

        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equalsIgnoreCase(code)) {
                return names[i];
            }
        }

        return vietnameseName ? "Tiếng Việt" : "Vietnamese";
    }

    public static int getLanguageIndexFromCode(String code) {
        if (code == null) {
            return 0;
        }

        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equalsIgnoreCase(code)) {
                return i;
            }
        }

        return 0;
    }

    public static String getLanguageCodeAt(int index) {
        if (index < 0 || index >= LANGUAGE_CODES.length) {
            return "vi";
        }

        return LANGUAGE_CODES[index];
    }

    public static String getDefaultLanguageName(Context context) {
        boolean vietnameseUi = !"en".equalsIgnoreCase(getAppLanguageCode(context));
        return getLanguageNameFromCode(getDefaultLanguageCode(context), vietnameseUi);
    }

    public static String getAppLanguageName(Context context) {
        boolean vietnameseUi = !"en".equalsIgnoreCase(getAppLanguageCode(context));
        return getLanguageNameFromCode(getAppLanguageCode(context), vietnameseUi);
    }
}