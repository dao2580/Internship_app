package vn.edu.usth.myapplication.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quiz_results",
        indices = {
                @Index(value = {"userEmail", "createdAt"}),
                @Index(value = {"userId", "createdAt"}),
                @Index(value = {"userId", "sessionId"})
        }
)
public class QuizResultEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /*
     * Old field.
     * Keep only for backward compatibility and old local data migration.
     * Do not use userEmail as the main owner key anymore.
     */
    public String userEmail;

    /*
     * New owner field.
     * This is the Supabase Auth user id.
     */
    public String userId;

    /*
     * Remote Supabase quiz_results.id
     */
    public String remoteId;

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String sessionId = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String questionType = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String targetLang = "";

    @NonNull
    @ColumnInfo(defaultValue = "''")
    public String wordLabelEn = "";

    public String question;
    public String correctAnswer;
    public String userAnswer;

    public boolean isCorrect;

    @ColumnInfo(defaultValue = "0")
    public int pointsEarned;

    @ColumnInfo(defaultValue = "0")
    public int maxPoints;

    public long createdAt;

    @ColumnInfo(defaultValue = "0")
    public boolean isSynced;

    @ColumnInfo(defaultValue = "0")
    public long updatedAt;

    @ColumnInfo(defaultValue = "0")
    public long deletedAt;

    public QuizResultEntity() {
        long now = System.currentTimeMillis();

        this.createdAt = now;
        this.updatedAt = now;
        this.deletedAt = 0;
        this.isSynced = false;
    }
}