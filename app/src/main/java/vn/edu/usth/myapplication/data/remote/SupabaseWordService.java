package vn.edu.usth.myapplication.data.remote;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import vn.edu.usth.myapplication.BuildConfig;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;

public class SupabaseWordService {

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final SupabaseSessionManager sessionManager;

    public SupabaseWordService(Context context) {
        this.sessionManager = new SupabaseSessionManager(context.getApplicationContext());
    }

    public String upsertWordBlocking(LearnedWordEntity word) throws Exception {
        String accessToken = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("Missing user id");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/learned_words?on_conflict=user_id,label_en,target_lang";

        JsonObject json = toJson(word, userId.trim());

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(RequestBody.create(json.toString(), JSON))
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            throw new RuntimeException("Upload word failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();

        if (array.size() == 0) {
            return null;
        }

        JsonObject first = array.get(0).getAsJsonObject();

        return getString(first, "id");
    }

    public List<LearnedWordEntity> downloadWordsBlocking(String email) throws Exception {
        String accessToken = sessionManager.getAccessToken();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/learned_words?select=*&order=last_seen_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            throw new RuntimeException("Download words failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();
        List<LearnedWordEntity> result = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            JsonObject json = array.get(i).getAsJsonObject();
            result.add(fromJson(json, email));
        }

        return result;
    }

    private JsonObject toJson(LearnedWordEntity word, String userId) {
        long now = System.currentTimeMillis();

        JsonObject json = new JsonObject();

        json.addProperty("user_id", userId);

        json.addProperty("label_en", safe(word.labelEn));
        json.addProperty("label_vi", safe(word.labelVi));
        json.addProperty("translated", safe(word.translated));
        json.addProperty("target_lang", safe(word.targetLang));
        json.addProperty("mode", safe(word.mode));

        json.addProperty("times_seen", word.timesSeen);
        json.addProperty("times_correct", word.timesCorrect);
        json.addProperty("times_wrong", word.timesWrong);
        json.addProperty("is_favorite", word.isFavorite);

        json.addProperty("created_at", word.createdAt > 0 ? word.createdAt : now);
        json.addProperty("last_seen_at", word.lastSeenAt > 0 ? word.lastSeenAt : now);
        json.addProperty("updated_at", word.updatedAt > 0 ? word.updatedAt : now);

        return json;
    }

    private LearnedWordEntity fromJson(JsonObject json, String email) {
        LearnedWordEntity word = new LearnedWordEntity();

        word.remoteId = getString(json, "id");
        word.userId = getString(json, "user_id");
        word.userEmail = email;

        word.labelEn = getString(json, "label_en");
        word.labelVi = getString(json, "label_vi");
        word.translated = getString(json, "translated");
        word.targetLang = getString(json, "target_lang");
        word.mode = getString(json, "mode");

        word.timesSeen = getInt(json, "times_seen");
        word.timesCorrect = getInt(json, "times_correct");
        word.timesWrong = getInt(json, "times_wrong");
        word.isFavorite = getBoolean(json, "is_favorite");

        word.createdAt = getLong(json, "created_at");
        word.lastSeenAt = getLong(json, "last_seen_at");
        word.updatedAt = getLong(json, "updated_at");

        word.isSynced = true;
        word.deletedAt = 0;

        return word;
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }

        return obj.get(key).getAsString();
    }

    private int getInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return 0;
        }

        return obj.get(key).getAsInt();
    }

    private long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return 0L;
        }

        return obj.get(key).getAsLong();
    }

    private boolean getBoolean(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return false;
        }

        return obj.get(key).getAsBoolean();
    }
}