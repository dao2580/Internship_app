package vn.edu.usth.myapplication.data.remote;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vn.edu.usth.myapplication.BuildConfig;

public class SupabaseAuthService {

    private static final String TAG = "SupabaseAuthService";

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AuthCallback {
        void onSuccess(SupabaseSession session);

        void onError(String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(String message);
    }

    public void signUp(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/auth/v1/signup";

                Log.d(TAG, "SIGN UP URL = " + url);

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("email", email);
                bodyJson.addProperty("password", password);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "SIGN UP FAILED: " + body);
                    postError(callback, parseError(body, "registration_failed"));
                    return;
                }

                /*
                 * If email confirmation is disabled in Supabase,
                 * we can sign in immediately after sign up.
                 */
                signIn(email, password, callback);

            } catch (Exception e) {
                Log.e(TAG, "SIGN UP EXCEPTION", e);
                postError(callback, "registration_failed");
            }
        });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/auth/v1/token?grant_type=password";

                Log.d(TAG, "SIGN IN URL = " + url);

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("email", email);
                bodyJson.addProperty("password", password);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "SIGN IN FAILED: " + body);
                    postError(callback, parseError(body, "login_failed"));
                    return;
                }

                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                SupabaseSession session = parseSession(root, email);

                if (session == null || isEmpty(session.accessToken) || isEmpty(session.userId)) {
                    Log.e(TAG, "SIGN IN FAILED: missing session fields");
                    postError(callback, "login_failed");
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(session));

            } catch (Exception e) {
                Log.e(TAG, "SIGN IN EXCEPTION", e);
                postError(callback, "login_failed");
            }
        });
    }

    public void sendPasswordResetEmail(String email, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/auth/v1/recover";

                Log.d(TAG, "RECOVER URL = " + url);

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("email", email);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "SEND RESET EMAIL FAILED: " + body);
                    postSimpleError(callback, parseError(body, "send_reset_email_failed"));
                    return;
                }

                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e(TAG, "SEND RESET EMAIL EXCEPTION", e);
                postSimpleError(callback, "send_reset_email_failed");
            }
        });
    }

    public void verifyRecoveryOtp(String email, String otp, AuthCallback callback) {
        executor.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/auth/v1/verify";

                Log.d(TAG, "VERIFY RECOVERY OTP URL = " + url);

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("email", email);
                bodyJson.addProperty("token", otp);
                bodyJson.addProperty("type", "recovery");

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "VERIFY RECOVERY OTP FAILED: " + body);
                    postError(callback, parseError(body, "invalid_or_expired_code"));
                    return;
                }

                JsonObject root = JsonParser.parseString(body).getAsJsonObject();
                SupabaseSession session = parseSession(root, email);

                if (session == null || isEmpty(session.accessToken)) {
                    Log.e(TAG, "VERIFY RECOVERY OTP FAILED: missing access token after OTP verification");
                    postError(callback, "missing_access_token_after_otp");
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(session));

            } catch (Exception e) {
                Log.e(TAG, "VERIFY RECOVERY OTP EXCEPTION", e);
                postError(callback, "invalid_or_expired_code");
            }
        });
    }

    public void updatePassword(String accessToken, String newPassword, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                if (isEmpty(accessToken)) {
                    Log.e(TAG, "UPDATE PASSWORD FAILED: access token is empty");
                    postSimpleError(callback, "missing_access_token");
                    return;
                }

                String url = BuildConfig.SUPABASE_URL + "/auth/v1/user";

                Log.d(TAG, "UPDATE PASSWORD URL = " + url);

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("password", newPassword);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .put(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    /*
                     * Do not log the new password.
                     * This body is safe to log for debugging because it only contains Supabase error info.
                     */
                    Log.e(TAG, "UPDATE PASSWORD FAILED: " + body);
                    postSimpleError(callback, parseError(body, "update_password_failed"));
                    return;
                }

                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e(TAG, "UPDATE PASSWORD EXCEPTION", e);
                postSimpleError(callback, "update_password_failed");
            }
        });
    }

    public void updateEmail(String accessToken, String newEmail, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                if (isEmpty(accessToken)) {
                    Log.e(TAG, "UPDATE EMAIL FAILED: access token is empty");
                    postSimpleError(callback, "missing_access_token");
                    return;
                }

                String url = BuildConfig.SUPABASE_URL + "/auth/v1/user";

                JsonObject bodyJson = new JsonObject();
                bodyJson.addProperty("email", newEmail);

                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .addHeader("Content-Type", "application/json")
                        .put(RequestBody.create(bodyJson.toString(), JSON))
                        .build();

                Response response = client.newCall(request).execute();
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "UPDATE EMAIL FAILED: " + body);
                    postSimpleError(callback, parseError(body, "update_email_failed"));
                    return;
                }

                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                Log.e(TAG, "UPDATE EMAIL EXCEPTION", e);
                postSimpleError(callback, "update_email_failed");
            }
        });
    }

    private SupabaseSession parseSession(JsonObject root, String fallbackEmail) {
        try {
            if (root == null) {
                return null;
            }

            /*
             * Supabase Auth usually returns access_token directly at root.
             * This also supports a nested "session" object if response shape changes.
             */
            JsonObject sessionObj = root;

            if (root.has("session") && root.get("session").isJsonObject()) {
                sessionObj = root.getAsJsonObject("session");
            }

            String accessToken = getString(sessionObj, "access_token");
            String refreshToken = getString(sessionObj, "refresh_token");

            JsonObject userObj = null;

            if (root.has("user") && root.get("user").isJsonObject()) {
                userObj = root.getAsJsonObject("user");
            } else if (sessionObj.has("user") && sessionObj.get("user").isJsonObject()) {
                userObj = sessionObj.getAsJsonObject("user");
            }

            String userId = getString(userObj, "id");
            String userEmail = getString(userObj, "email");

            return new SupabaseSession(
                    accessToken,
                    refreshToken,
                    userId,
                    userEmail != null ? userEmail : fallbackEmail
            );

        } catch (Exception e) {
            Log.e(TAG, "PARSE SESSION FAILED", e);
            return null;
        }
    }

    private void postError(AuthCallback callback, String message) {
        mainHandler.post(() ->
                callback.onError(message != null ? message : "unknown_error")
        );
    }

    private void postSimpleError(SimpleCallback callback, String message) {
        mainHandler.post(() ->
                callback.onError(message != null ? message : "unknown_error")
        );
    }

    private String parseError(String body, String fallback) {
        try {
            if (isEmpty(body)) {
                return fallback;
            }

            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            /*
             * Prefer machine-readable codes first.
             * UI can map these codes to R.string values.
             */
            if (root.has("error_code") && !root.get("error_code").isJsonNull()) {
                return root.get("error_code").getAsString();
            }

            if (root.has("code") && !root.get("code").isJsonNull()) {
                return root.get("code").getAsString();
            }

            if (root.has("msg") && !root.get("msg").isJsonNull()) {
                return root.get("msg").getAsString();
            }

            if (root.has("message") && !root.get("message").isJsonNull()) {
                return root.get("message").getAsString();
            }

            if (root.has("error_description") && !root.get("error_description").isJsonNull()) {
                return root.get("error_description").getAsString();
            }

            if (root.has("error") && !root.get("error").isJsonNull()) {
                return root.get("error").getAsString();
            }

            return fallback;

        } catch (Exception e) {
            return fallback;
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }

        return obj.get(key).getAsString();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}