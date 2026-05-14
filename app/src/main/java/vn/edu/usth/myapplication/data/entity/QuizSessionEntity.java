package vn.edu.usth.myapplication.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_sessions",
        indices = {
                @Index(value = {"userEmail", "createdAt"}),
                @Index(value = {"userId", "createdAt"})
        }
)
public class QuizSessionEntity {

    @PrimaryKey
    @NonNull
    public String sessionId = "";

    /*
     * Old field.
     * Keep only for backward compatibility and old local data migration.
     * Do not use userEmail as the main owner key anymore.
     */
    public String userEmail;

    /*
     * New owner field.
     * This is the Supabase Auth user id.
     * All new quiz session queries should use userId.
     */
    public String userId;

    /*
     * Remote Supabase quiz_sessions.id
     */
    public String remoteId;

    public String targetLang;
    public String sourceMode;

    public int totalQuestions;
    public int correctAnswers;
    public int earnedPoints;
    public int maxPoints;

    public long createdAt;

    @ColumnInfo(defaultValue = "0")
    public boolean isSynced;

    @ColumnInfo(defaultValue = "0")
    public long updatedAt;

    @ColumnInfo(defaultValue = "0")
    public long deletedAt;

    public QuizSessionEntity() {
        long now = System.currentTimeMillis();

        this.createdAt = now;
        this.updatedAt = now;
        this.deletedAt = 0;
        this.isSynced = false;
    }
}