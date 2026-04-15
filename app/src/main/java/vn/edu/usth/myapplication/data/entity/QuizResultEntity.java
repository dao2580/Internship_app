package vn.edu.usth.myapplication.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_results")
public class QuizResultEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userEmail;
    public String question;
    public String correctAnswer;
    public String userAnswer;
    public boolean isCorrect;
    public long createdAt;

    public QuizResultEntity() {
        this.createdAt = System.currentTimeMillis();
    }
}