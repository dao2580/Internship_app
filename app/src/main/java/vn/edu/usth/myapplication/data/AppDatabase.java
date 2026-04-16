package vn.edu.usth.myapplication.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import vn.edu.usth.myapplication.data.dao.LearnedWordDao;
import vn.edu.usth.myapplication.data.dao.QuizResultDao;
import vn.edu.usth.myapplication.data.dao.QuizSessionDao;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

@Database(
        entities = {
                LearnedWordEntity.class,
                QuizResultEntity.class,
                QuizSessionEntity.class
        },
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract LearnedWordDao learnedWordDao();
    public abstract QuizResultDao quizResultDao();
    public abstract QuizSessionDao quizSessionDao();

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN sessionId TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN questionType TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN targetLang TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN wordLabelEn TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN pointsEarned INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE quiz_results ADD COLUMN maxPoints INTEGER NOT NULL DEFAULT 0");

            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `quiz_sessions` (" +
                            "`sessionId` TEXT NOT NULL, " +
                            "`userEmail` TEXT, " +
                            "`targetLang` TEXT, " +
                            "`sourceMode` TEXT, " +
                            "`totalQuestions` INTEGER NOT NULL, " +
                            "`correctAnswers` INTEGER NOT NULL, " +
                            "`earnedPoints` INTEGER NOT NULL, " +
                            "`maxPoints` INTEGER NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`sessionId`))"
            );
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE learned_words ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "camStudy.db"
                            )
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}