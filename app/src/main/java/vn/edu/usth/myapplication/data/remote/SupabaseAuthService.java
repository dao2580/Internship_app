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
                    postError(callback, parseError(body, "Registration failed"));
                    return;
                }

                /*
                 * If email confirmation is disabled in Supabase,
                 * we can sign in immediately after sign up.
                 */
                signIn(email, password, callback);

            } catch (Exception e) {
                postError(callback, e.getMessage());
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
                    postError(callback, parseError(body, "Login failed"));
                    return;
                }

                JsonObject root = JsonParser.parseString(body).getAsJsonObject();

                String accessToken = getString(root, "access_token");
                String refreshToken = getString(root, "refresh_token");

                JsonObject user = root.getAsJsonObject("user");
                String userId = getString(user, "id");
                String userEmail = getString(user, "email");

                SupabaseSession session = new SupabaseSession(
                        accessToken,
                        refreshToken,
                        userId,
                        userEmail != null ? userEmail : email
                );

                mainHandler.post(() -> callback.onSuccess(session));

            } catch (Exception e) {
                postError(callback, e.getMessage());
            }
        });
    }

    public void sendPasswordResetEmail(String email, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                String url = BuildConfig.SUPABASE_URL + "/auth/v1/recover";

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
                    mainHandler.post(() ->
                            callback.onError(parseError(body, "Cannot send reset email"))
                    );
                    return;
                }

                mainHandler.post(callback::onSuccess);

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private void postError(AuthCallback callback, String message) {
        mainHandler.post(() ->
                callback.onError(message != null ? message : "Unknown error")
        );
    }

    private String parseError(String body, String fallback) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            if (root.has("msg")) {
                return root.get("msg").getAsString();
            }

            if (root.has("message")) {
                return root.get("message").getAsString();
            }

            if (root.has("error_description")) {
                return root.get("error_description").getAsString();
            }

            if (root.has("error")) {
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
}