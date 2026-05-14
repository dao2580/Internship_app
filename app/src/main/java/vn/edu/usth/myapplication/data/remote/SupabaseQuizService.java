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
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

public class SupabaseQuizService {

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private final SupabaseSessionManager sessionManager;

    public SupabaseQuizService(Context context) {
        sessionManager = new SupabaseSessionManager(context.getApplicationContext());
    }

    public String upsertSessionBlocking(QuizSessionEntity session) throws Exception {
        String accessToken = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("Missing user id");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/quiz_sessions?on_conflict=user_id,local_session_id";

        JsonObject json = sessionToJson(session, userId.trim());

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
            throw new RuntimeException("Upload quiz session failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();

        if (array.size() == 0) {
            return null;
        }

        return getString(array.get(0).getAsJsonObject(), "id");
    }

    public String upsertResultBlocking(QuizResultEntity result) throws Exception {
        String accessToken = sessionManager.getAccessToken();
        String userId = sessionManager.getUserId();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("Missing user id");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/quiz_results?on_conflict=user_id,local_id";

        JsonObject json = resultToJson(result, userId.trim());

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
            throw new RuntimeException("Upload quiz result failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();

        if (array.size() == 0) {
            return null;
        }

        return getString(array.get(0).getAsJsonObject(), "id");
    }

    public List<QuizSessionEntity> downloadSessionsBlocking(String email) throws Exception {
        String accessToken = sessionManager.getAccessToken();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/quiz_sessions?select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            throw new RuntimeException("Download quiz sessions failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();
        List<QuizSessionEntity> result = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            result.add(sessionFromJson(array.get(i).getAsJsonObject(), email));
        }

        return result;
    }

    public List<QuizResultEntity> downloadResultsBlocking(String email) throws Exception {
        String accessToken = sessionManager.getAccessToken();

        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("Missing access token");
        }

        String url = BuildConfig.SUPABASE_URL
                + "/rest/v1/quiz_results?select=*&order=created_at.desc";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + accessToken)
                .get()
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body() != null ? response.body().string() : "";

        if (!response.isSuccessful()) {
            throw new RuntimeException("Download quiz results failed: " + body);
        }

        JsonArray array = JsonParser.parseString(body).getAsJsonArray();
        List<QuizResultEntity> result = new ArrayList<>();

        for (int i = 0; i < array.size(); i++) {
            result.add(resultFromJson(array.get(i).getAsJsonObject(), email));
        }

        return result;
    }

    private JsonObject sessionToJson(QuizSessionEntity session, String userId) {
        long now = System.currentTimeMillis();

        JsonObject json = new JsonObject();

        json.addProperty("user_id", userId);
        json.addProperty("local_session_id", safe(session.sessionId));

        json.addProperty("target_lang", safe(session.targetLang));
        json.addProperty("source_mode", safe(session.sourceMode));

        json.addProperty("total_questions", session.totalQuestions);
        json.addProperty("correct_answers", session.correctAnswers);
        json.addProperty("earned_points", session.earnedPoints);
        json.addProperty("max_points", session.maxPoints);

        json.addProperty("created_at", session.createdAt > 0 ? session.createdAt : now);
        json.addProperty("updated_at", session.updatedAt > 0 ? session.updatedAt : now);

        return json;
    }

    private JsonObject resultToJson(QuizResultEntity result, String userId) {
        long now = System.currentTimeMillis();

        JsonObject json = new JsonObject();

        json.addProperty("user_id", userId);
        json.addProperty("local_id", result.id);
        json.addProperty("local_session_id", safe(result.sessionId));

        json.addProperty("question_type", safe(result.questionType));
        json.addProperty("target_lang", safe(result.targetLang));
        json.addProperty("word_label_en", safe(result.wordLabelEn));

        json.addProperty("question", safe(result.question));
        json.addProperty("correct_answer", safe(result.correctAnswer));
        json.addProperty("user_answer", safe(result.userAnswer));
        json.addProperty("is_correct", result.isCorrect);

        json.addProperty("points_earned", result.pointsEarned);
        json.addProperty("max_points", result.maxPoints);

        json.addProperty("created_at", result.createdAt > 0 ? result.createdAt : now);
        json.addProperty("updated_at", result.updatedAt > 0 ? result.updatedAt : now);

        return json;
    }

    private QuizSessionEntity sessionFromJson(JsonObject json, String email) {
        QuizSessionEntity session = new QuizSessionEntity();

        String localSessionId = getString(json, "local_session_id");

        session.remoteId = getString(json, "id");
        session.userId = getString(json, "user_id");
        session.userEmail = email;
        session.sessionId = localSessionId != null ? localSessionId : getString(json, "id");

        session.targetLang = getString(json, "target_lang");
        session.sourceMode = getString(json, "source_mode");

        session.totalQuestions = getInt(json, "total_questions");
        session.correctAnswers = getInt(json, "correct_answers");
        session.earnedPoints = getInt(json, "earned_points");
        session.maxPoints = getInt(json, "max_points");

        session.createdAt = getLong(json, "created_at");
        session.updatedAt = getLong(json, "updated_at");

        session.isSynced = true;
        session.deletedAt = 0;

        return session;
    }

    private QuizResultEntity resultFromJson(JsonObject json, String email) {
        QuizResultEntity result = new QuizResultEntity();

        result.remoteId = getString(json, "id");
        result.userId = getString(json, "user_id");
        result.userEmail = email;

        result.id = getInt(json, "local_id");
        result.sessionId = safe(getString(json, "local_session_id"));

        result.questionType = safe(getString(json, "question_type"));
        result.targetLang = safe(getString(json, "target_lang"));
        result.wordLabelEn = safe(getString(json, "word_label_en"));

        result.question = getString(json, "question");
        result.correctAnswer = getString(json, "correct_answer");
        result.userAnswer = getString(json, "user_answer");
        result.isCorrect = getBoolean(json, "is_correct");

        result.pointsEarned = getInt(json, "points_earned");
        result.maxPoints = getInt(json, "max_points");

        result.createdAt = getLong(json, "created_at");
        result.updatedAt = getLong(json, "updated_at");

        result.isSynced = true;
        result.deletedAt = 0;

        return result;
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