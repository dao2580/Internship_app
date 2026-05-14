package vn.edu.usth.myapplication.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

public class SupabaseSessionManager {

    private static final String PREFS_NAME = "SupabaseSessionPrefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";

    private final SharedPreferences prefs;

    public SupabaseSessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(SupabaseSession session) {
        if (session == null) return;

        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, session.accessToken)
                .putString(KEY_REFRESH_TOKEN, session.refreshToken)
                .putString(KEY_USER_ID, session.userId)
                .putString(KEY_EMAIL, session.email)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        String accessToken = getAccessToken();
        String userId = getUserId();

        return accessToken != null && !accessToken.trim().isEmpty()
                && userId != null && !userId.trim().isEmpty();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}