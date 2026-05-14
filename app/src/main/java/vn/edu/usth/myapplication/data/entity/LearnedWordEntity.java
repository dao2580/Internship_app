package vn.edu.usth.myapplication.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "learned_words",
        indices = {
                @Index(value = {"userEmail", "labelEn", "targetLang"}, unique = true),
                @Index(value = {"userId", "labelEn", "targetLang"})
        }
)
public class LearnedWordEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /*
     * Old owner field.
     * Keep it for backward compatibility and migration from old local data.
     */
    public String userEmail;

    /*
     * New owner field.
     * This is the Supabase Auth user id.
     * All new queries should use userId instead of userEmail.
     */
    public String userId;

    /*
     * Remote id from Supabase learned_words.id
     */
    public String remoteId;

    public String labelEn;
    public String labelVi;
    public String translated;
    public String targetLang;
    public String mode;

    public int timesSeen;
    public int timesCorrect;
    public int timesWrong;

    public boolean isFavorite;

    @ColumnInfo(defaultValue = "0")
    public boolean isSynced;

    public long createdAt;
    public long lastSeenAt;

    @ColumnInfo(defaultValue = "0")
    public long updatedAt;

    @ColumnInfo(defaultValue = "0")
    public long deletedAt;

    public LearnedWordEntity() {
        long now = System.currentTimeMillis();

        this.timesSeen = 1;
        this.timesCorrect = 0;
        this.timesWrong = 0;

        this.isFavorite = false;
        this.isSynced = false;

        this.createdAt = now;
        this.lastSeenAt = now;
        this.updatedAt = now;
        this.deletedAt = 0;
    }
}