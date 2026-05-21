package vn.edu.usth.myapplication;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    public static Context applyLocale(Context context) {
        String languageCode = SettingsPreferences.getAppLanguageCode(context);
        return updateResources(context, languageCode);
    }

    public static Context setLocale(Context context, String languageCode) {
        SettingsPreferences.setAppLanguageCode(context, languageCode);
        return updateResources(context, languageCode);
    }

    private static Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            return context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            resources.updateConfiguration(config, resources.getDisplayMetrics());
            return context;
        }
    }
}