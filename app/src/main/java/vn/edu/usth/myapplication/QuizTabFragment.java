package vn.edu.usth.myapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import vn.edu.usth.myapplication.data.AppRepository;
import vn.edu.usth.myapplication.data.entity.LearnedWordEntity;
import vn.edu.usth.myapplication.data.entity.QuizResultEntity;
import vn.edu.usth.myapplication.data.entity.QuizSessionEntity;

public class QuizTabFragment extends Fragment implements QuizWrongReviewAdapter.OnSpeakCorrectAnswerListener {

    private enum QuestionType {
        MCQ_EN_TO_TARGET("MCQ: English → Target", 1),
        MCQ_TARGET_TO_EN("MCQ: Target → English", 1),
        TYPE_EN_FROM_TARGET("Typing", 2),
        TRUE_FALSE("True / False", 1),
        UNSCRAMBLE_EN("Unscramble", 2),
        AUDIO_TARGET_TO_EN("Audio listen", 2);

        final String label;
        final int points;

        QuestionType(String label, int points) {
            this.label = label;
            this.points = points;
        }
    }

    private static class QuizWord {
        int wordId;
        String labelEn;
        String translated;
        String targetLang;
        boolean fromLearnedDb;

        QuizWord(int wordId, String labelEn, String translated, String targetLang, boolean fromLearnedDb) {
            this.wordId = wordId;
            this.labelEn = labelEn;
            this.translated = translated;
            this.targetLang = targetLang;
            this.fromLearnedDb = fromLearnedDb;
        }
    }

    private static class QuizQuestion {
        final QuestionType type;
        final QuizWord quizWord;
        final String prompt;
        final String correctAnswer;
        final List<String> options;
        final String audioText;

        QuizQuestion(QuestionType type, QuizWord quizWord, String prompt,
                     String correctAnswer, List<String> options, String audioText) {
            this.type = type;
            this.quizWord = quizWord;
            this.prompt = prompt;
            this.correctAnswer = correctAnswer;
            this.options = options;
            this.audioText = audioText;
        }
    }

    private static final String[][] LANGS = {
            {"Vietnamese", "vi"},
            {"English", "en"},
            {"Chinese", "zh"},
            {"Japanese", "ja"},
            {"Korean", "ko"},
            {"French", "fr"},
            {"German", "de"},
            {"Spanish", "es"},
            {"Thai", "th"},
            {"Russian", "ru"}
    };

    private TextView txtScore, txtQuestion, txtQNumber, txtEmpty, txtQuestionType;
    private TextView txtQuizSourceHint, txtLastQuizReview, txtRecentSessions;

    private MaterialButton btnGenerate, btnA, btnB, btnC, btnD;
    private MaterialButton btnSubmitText, btnPlayAudio;

    private View cardQuiz;
    private ProgressBar progressBar;
    private AutoCompleteTextView spinnerQuizLanguage, spinnerQuizCount;
    private TextInputLayout tilTypeAnswer;
    private TextInputEditText etTypeAnswer;

    private RecyclerView recyclerRecentWrongs;
    private QuizWrongReviewAdapter wrongReviewAdapter;

    private final LinkedHashMap<String, String> languageMap = new LinkedHashMap<>();
    private final List<String> languageNames = new ArrayList<>();

    private final List<QuizQuestion> questions = new ArrayList<>();
    private final List<QuizResultEntity> currentWrongResults = new ArrayList<>();

    private int currentIndex = 0;
    private int correctCount = 0;
    private int earnedPoints = 0;
    private int maxPoints = 0;
    private int questionCount = 10;

    private String currentTargetCode = "vi";
    private String currentSourceMode = "Starter Pack";
    private String currentSessionId = "";

    private AppRepository repository;
    private final Random random = new Random();

    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_tab_quiz, container, false);

        repository = new AppRepository(requireContext());

        txtScore = v.findViewById(R.id.txt_score);
        txtQuestion = v.findViewById(R.id.txt_question);
        txtQNumber = v.findViewById(R.id.txt_question_number);
        txtEmpty = v.findViewById(R.id.txt_empty);
        txtQuestionType = v.findViewById(R.id.txt_question_type);
        txtQuizSourceHint = v.findViewById(R.id.txt_quiz_source_hint);
        txtLastQuizReview = v.findViewById(R.id.txt_last_quiz_review);
        txtRecentSessions = v.findViewById(R.id.txt_recent_sessions);

        btnGenerate = v.findViewById(R.id.btn_generate_quiz);
        btnA = v.findViewById(R.id.btn_option_a);
        btnB = v.findViewById(R.id.btn_option_b);
        btnC = v.findViewById(R.id.btn_option_c);
        btnD = v.findViewById(R.id.btn_option_d);
        btnSubmitText = v.findViewById(R.id.btn_submit_text);
        btnPlayAudio = v.findViewById(R.id.btn_play_audio);

        cardQuiz = v.findViewById(R.id.card_quiz);
        progressBar = v.findViewById(R.id.progress_quiz);
        spinnerQuizLanguage = v.findViewById(R.id.spinner_quiz_language);
        spinnerQuizCount = v.findViewById(R.id.spinner_quiz_count);
        tilTypeAnswer = v.findViewById(R.id.til_type_answer);
        etTypeAnswer = v.findViewById(R.id.et_type_answer);

        recyclerRecentWrongs = v.findViewById(R.id.recycler_recent_wrongs);
        recyclerRecentWrongs.setLayoutManager(new LinearLayoutManager(getContext()));
        wrongReviewAdapter = new QuizWrongReviewAdapter(this);
        recyclerRecentWrongs.setAdapter(wrongReviewAdapter);

        setupDropdowns();
        setupTts();
        loadScore();
        refreshSourceHint();
        observeReviewPanels();

        btnGenerate.setOnClickListener(x -> generateQuiz());

        return v;
    }

    private void setupDropdowns() {
        for (String[] row : LANGS) {
            languageMap.put(row[0], row[1]);
            languageNames.add(row[0]);
        }

        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                languageNames
        );
        spinnerQuizLanguage.setAdapter(langAdapter);
        spinnerQuizLanguage.setText("Vietnamese", false);
        currentTargetCode = "vi";

        List<String> counts = new ArrayList<>();
        counts.add("10");
        counts.add("15");
        counts.add("20");

        ArrayAdapter<String> countAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                counts
        );
        spinnerQuizCount.setAdapter(countAdapter);
        spinnerQuizCount.setText("10", false);
        questionCount = 10;

        spinnerQuizLanguage.setOnItemClickListener((parent, view, position, id) -> {
            String name = languageNames.get(position);
            String code = languageMap.get(name);
            if (code != null) {
                currentTargetCode = code;
                refreshSourceHint();
            }
        });

        spinnerQuizCount.setOnItemClickListener((parent, view, position, id) -> {
            try {
                questionCount = Integer.parseInt(counts.get(position));
            } catch (Exception ignored) {
                questionCount = 10;
            }
        });
    }

    private void setupTts() {
        releaseTts();
        tts = new TextToSpeech(requireContext(), status -> {
            ttsReady = (status == TextToSpeech.SUCCESS);
        });
    }

    private void releaseTts() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {
            }
        }
        tts = null;
        ttsReady = false;
    }

    private void speakText(String text, String langCode) {
        if (text == null || text.trim().isEmpty()) {
            toast("Không có audio");
            return;
        }

        if (tts == null || !ttsReady) {
            setupTts();
            toast("TTS đang khởi động, bấm lại");
            return;
        }

        try {
            Locale locale = Locale.forLanguageTag(langCode);
            int result = tts.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }

            tts.stop();
            tts.setSpeechRate(1.0f);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "QUIZ_AUDIO");
        } catch (Exception e) {
            setupTts();
            toast("TTS vừa reset, bấm lại");
        }
    }

    @Override
    public void onSpeakCorrectAnswer(QuizResultEntity item) {
        String langToSpeak = getSpeechLanguageForWrongItem(item);
        speakText(item.correctAnswer, langToSpeak);
    }

    private String getSpeechLanguageForWrongItem(QuizResultEntity item) {
        if (item == null || item.questionType == null) return "en";

        switch (item.questionType) {
            case "MCQ_EN_TO_TARGET":
                return item.targetLang != null && !item.targetLang.isEmpty() ? item.targetLang : "vi";
            case "TRUE_FALSE":
                return "en";
            case "MCQ_TARGET_TO_EN":
            case "TYPE_EN_FROM_TARGET":
            case "UNSCRAMBLE_EN":
            case "AUDIO_TARGET_TO_EN":
            default:
                return "en";
        }
    }

    private void loadScore() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;

        repository.getQuizStats(email, stats ->
                requireActivity().runOnUiThread(() -> {
                    int correct = stats[0];
                    int total = stats[1];
                    int wrong = total - correct;
                    txtScore.setText("Tổng quiz: Đúng " + correct + " | Sai " + wrong);
                })
        );
    }

    private void observeReviewPanels() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) return;

        repository.getRecentQuizSessionsLive(email).observe(getViewLifecycleOwner(), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                txtRecentSessions.setText("Chưa có dữ liệu.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            int limit = Math.min(sessions.size(), 5);

            for (int i = 0; i < limit; i++) {
                QuizSessionEntity s = sessions.get(i);
                int wrong = s.totalQuestions - s.correctAnswers;

                if (i > 0) sb.append("\n\n");
                sb.append("• ")
                        .append(df.format(new Date(s.createdAt)))
                        .append(" | ")
                        .append(s.targetLang)
                        .append(" | ")
                        .append(s.sourceMode)
                        .append("\n")
                        .append("Đúng: ")
                        .append(s.correctAnswers)
                        .append(" | Sai: ")
                        .append(wrong);
            }

            txtRecentSessions.setText(sb.toString());
        });

        repository.getRecentWrongQuizResultsLive(email).observe(getViewLifecycleOwner(), wrongs -> {
            wrongReviewAdapter.submitList(wrongs);
        });
    }

    private void refreshSourceHint() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) {
            txtQuizSourceHint.setText("Chưa đăng nhập.");
            return;
        }

        txtQuizSourceHint.setText("Đang phân tích dữ liệu quiz...");

        repository.getAllWords(email, words -> {
            int learnedCount = 0;
            for (LearnedWordEntity w : words) {
                if (isSameLang(w.targetLang, currentTargetCode)) {
                    learnedCount++;
                }
            }

            if (learnedCount < 5) {
                currentSourceMode = "Starter Pack ưu tiên";
            } else if (learnedCount < 15) {
                currentSourceMode = "Mixed";
            } else {
                currentSourceMode = "My Words ưu tiên";
            }

            int starterCount = QuizStarterBank.getWords(currentTargetCode).size();

            String sourceText = "Nguồn quiz: " + currentSourceMode +
                    " | Learned: " + learnedCount +
                    " | Starter: " + starterCount;

            requireActivity().runOnUiThread(() -> txtQuizSourceHint.setText(sourceText));
        });
    }

    private void generateQuiz() {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email == null) {
            toast("Bạn cần đăng nhập trước");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        txtEmpty.setVisibility(View.GONE);
        cardQuiz.setVisibility(View.GONE);

        repository.getAllWords(email, words -> {
            List<QuizWord> learnedWords = toLearnedQuizWords(words, currentTargetCode);
            List<QuizWord> starterWords = toStarterQuizWords(currentTargetCode);

            if (learnedWords.isEmpty() && starterWords.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnGenerate.setEnabled(true);
                    txtEmpty.setText("Không có dữ liệu để tạo quiz.");
                    txtEmpty.setVisibility(View.VISIBLE);
                });
                return;
            }

            if (learnedWords.size() < 5) {
                currentSourceMode = "Starter Pack ưu tiên";
            } else if (learnedWords.size() < 15) {
                currentSourceMode = "Mixed";
            } else {
                currentSourceMode = "My Words ưu tiên";
            }

            List<QuizQuestion> generated = buildQuestions(learnedWords, starterWords, questionCount);

            requireActivity().runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnGenerate.setEnabled(true);

                questions.clear();
                questions.addAll(generated);

                currentIndex = 0;
                correctCount = 0;
                earnedPoints = 0;
                maxPoints = 0;
                currentWrongResults.clear();
                currentSessionId = UUID.randomUUID().toString();

                if (questions.isEmpty()) {
                    txtEmpty.setText("Không đủ dữ liệu để tạo quiz cho ngôn ngữ này.");
                    txtEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                txtEmpty.setVisibility(View.GONE);
                cardQuiz.setVisibility(View.VISIBLE);
                showQuestion();
            });
        });
    }

    private List<QuizWord> toLearnedQuizWords(List<LearnedWordEntity> words, String targetLang) {
        List<QuizWord> out = new ArrayList<>();

        for (LearnedWordEntity word : words) {
            if (!isSameLang(word.targetLang, targetLang)) continue;

            String translated = safeTranslated(word, targetLang);
            if (isBlank(word.labelEn) || isBlank(translated)) continue;

            out.add(new QuizWord(
                    word.id,
                    word.labelEn.trim(),
                    translated.trim(),
                    targetLang,
                    true
            ));
        }

        return out;
    }

    private List<QuizWord> toStarterQuizWords(String targetLang) {
        List<QuizStarterBank.StarterWord> starter = QuizStarterBank.getWords(targetLang);
        List<QuizWord> out = new ArrayList<>();

        for (QuizStarterBank.StarterWord word : starter) {
            out.add(new QuizWord(
                    -1,
                    word.labelEn,
                    word.translated,
                    word.targetLang,
                    false
            ));
        }

        return out;
    }

    private List<QuizQuestion> buildQuestions(List<QuizWord> learned, List<QuizWord> starter, int desiredCount) {
        List<QuizQuestion> out = new ArrayList<>();
        List<QuizWord> combined = new ArrayList<>(starter);
        mergeUniqueByEnglish(combined, learned);

        if (combined.size() < 4) return out;

        int safety = 0;
        while (out.size() < desiredCount && safety < 500) {
            safety++;

            QuizWord seed = pickSeed(learned, combined);
            QuestionType type = pickQuestionType(out.size());

            QuizQuestion q = buildQuestion(seed, combined, type);
            if (q != null) {
                out.add(q);
            }
        }

        return out;
    }

    private QuizWord pickSeed(List<QuizWord> learned, List<QuizWord> combined) {
        if (!learned.isEmpty() && random.nextFloat() < 0.7f) {
            return learned.get(random.nextInt(learned.size()));
        }
        return combined.get(random.nextInt(combined.size()));
    }

    private QuestionType pickQuestionType(int index) {
        QuestionType[] types = {
                QuestionType.MCQ_EN_TO_TARGET,
                QuestionType.MCQ_TARGET_TO_EN,
                QuestionType.TYPE_EN_FROM_TARGET,
                QuestionType.TRUE_FALSE,
                QuestionType.UNSCRAMBLE_EN,
                QuestionType.AUDIO_TARGET_TO_EN
        };
        return types[index % types.length];
    }

    @Nullable
    private QuizQuestion buildQuestion(QuizWord seed, List<QuizWord> pool, QuestionType type) {
        switch (type) {
            case MCQ_EN_TO_TARGET: {
                List<String> options = buildWrongTranslatedOptions(pool, seed, 3);
                if (options.size() < 3) return null;
                options.add(seed.translated);
                Collections.shuffle(options);

                return new QuizQuestion(
                        type,
                        seed,
                        "'" + seed.labelEn + "' nghĩa là gì trong ngôn ngữ đã chọn?",
                        seed.translated,
                        options,
                        null
                );
            }

            case MCQ_TARGET_TO_EN: {
                List<String> options = buildWrongEnglishOptions(pool, seed, 3);
                if (options.size() < 3) return null;
                options.add(seed.labelEn);
                Collections.shuffle(options);

                return new QuizQuestion(
                        type,
                        seed,
                        "'" + seed.translated + "' trong English là gì?",
                        seed.labelEn,
                        options,
                        null
                );
            }

            case TYPE_EN_FROM_TARGET: {
                return new QuizQuestion(
                        type,
                        seed,
                        "Gõ từ tiếng Anh của: " + seed.translated,
                        seed.labelEn,
                        new ArrayList<>(),
                        null
                );
            }

            case TRUE_FALSE: {
                boolean showCorrectPair = random.nextBoolean();
                String shownMeaning = showCorrectPair
                        ? seed.translated
                        : pickWrongTranslated(pool, seed);

                if (isBlank(shownMeaning)) return null;

                List<String> options = new ArrayList<>();
                options.add("True");
                options.add("False");

                return new QuizQuestion(
                        type,
                        seed,
                        "True or False:\n'" + seed.labelEn + "' = '" + shownMeaning + "'",
                        showCorrectPair ? "True" : "False",
                        options,
                        null
                );
            }

            case UNSCRAMBLE_EN: {
                String shuffled = shuffleLetters(seed.labelEn);
                if (shuffled.equalsIgnoreCase(seed.labelEn) && seed.labelEn.length() > 1) {
                    shuffled = shuffleLetters(seed.labelEn);
                }

                return new QuizQuestion(
                        type,
                        seed,
                        "Sắp xếp lại chữ cái để tạo từ tiếng Anh của '" +
                                seed.translated + "': " + shuffled,
                        seed.labelEn,
                        new ArrayList<>(),
                        null
                );
            }

            case AUDIO_TARGET_TO_EN: {
                List<String> options = buildWrongEnglishOptions(pool, seed, 3);
                if (options.size() < 3) return null;
                options.add(seed.labelEn);
                Collections.shuffle(options);

                return new QuizQuestion(
                        type,
                        seed,
                        "Nghe từ và chọn English đúng",
                        seed.labelEn,
                        options,
                        seed.translated
                );
            }
        }

        return null;
    }

    private void showQuestion() {
        if (currentIndex >= questions.size()) {
            finishQuiz();
            return;
        }

        QuizQuestion q = questions.get(currentIndex);
        maxPoints += q.type.points;

        txtQNumber.setText("Câu " + (currentIndex + 1) + "/" + questions.size());
        txtQuestionType.setText(q.type.label);
        txtQuestion.setText(q.prompt);

        resetButtons();

        tilTypeAnswer.setVisibility(View.GONE);
        btnSubmitText.setVisibility(View.GONE);
        btnPlayAudio.setVisibility(View.GONE);

        btnA.setVisibility(View.GONE);
        btnB.setVisibility(View.GONE);
        btnC.setVisibility(View.GONE);
        btnD.setVisibility(View.GONE);

        if (q.type == QuestionType.MCQ_EN_TO_TARGET
                || q.type == QuestionType.MCQ_TARGET_TO_EN
                || q.type == QuestionType.AUDIO_TARGET_TO_EN) {

            if (q.type == QuestionType.AUDIO_TARGET_TO_EN) {
                btnPlayAudio.setVisibility(View.VISIBLE);
                btnPlayAudio.setOnClickListener(v -> speakText(q.audioText, currentTargetCode));
            }

            showOptions(q.options, q);
        } else if (q.type == QuestionType.TRUE_FALSE) {
            List<String> tf = new ArrayList<>();
            tf.add("True");
            tf.add("False");
            showOptions(tf, q);
        } else {
            tilTypeAnswer.setVisibility(View.VISIBLE);
            btnSubmitText.setVisibility(View.VISIBLE);
            etTypeAnswer.setText("");
            etTypeAnswer.setEnabled(true);
            btnSubmitText.setEnabled(true);

            btnSubmitText.setOnClickListener(v -> {
                String typed = etTypeAnswer.getText() == null
                        ? ""
                        : etTypeAnswer.getText().toString().trim();

                if (typed.isEmpty()) {
                    toast("Hãy nhập câu trả lời");
                    return;
                }

                handleAnswer(typed, q);
            });
        }
    }

    private void showOptions(List<String> options, QuizQuestion q) {
        MaterialButton[] btns = {btnA, btnB, btnC, btnD};

        for (int i = 0; i < btns.length; i++) {
            MaterialButton btn = btns[i];
            if (i < options.size()) {
                String chosen = options.get(i);
                btn.setVisibility(View.VISIBLE);
                btn.setText(chosen);
                btn.setEnabled(true);
                btn.setOnClickListener(v -> handleAnswer(chosen, q));
            } else {
                btn.setVisibility(View.GONE);
            }
        }
    }

    private void handleAnswer(String chosen, QuizQuestion q) {
        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        boolean correct = isAnswerCorrect(chosen, q.correctAnswer);
        int earned = correct ? q.type.points : 0;

        if (correct) {
            correctCount++;
            earnedPoints += earned;
        }

        if (q.type == QuestionType.MCQ_EN_TO_TARGET
                || q.type == QuestionType.MCQ_TARGET_TO_EN
                || q.type == QuestionType.TRUE_FALSE
                || q.type == QuestionType.AUDIO_TARGET_TO_EN) {
            highlightMcq(chosen, q.correctAnswer);
        } else {
            etTypeAnswer.setEnabled(false);
            btnSubmitText.setEnabled(false);
        }

        if (email != null) {
            repository.saveQuizResult(
                    email,
                    currentSessionId,
                    q.type.name(),
                    currentTargetCode,
                    q.quizWord.labelEn,
                    q.prompt,
                    q.correctAnswer,
                    chosen,
                    correct,
                    earned,
                    q.type.points
            );

            if (q.quizWord.fromLearnedDb && q.quizWord.wordId > 0) {
                if (correct) repository.markCorrect(q.quizWord.wordId);
                else repository.markWrong(q.quizWord.wordId);
            } else if (correct) {
                repository.saveLearnedWord(
                        email,
                        q.quizWord.labelEn,
                        q.quizWord.translated,
                        q.quizWord.translated,
                        q.quizWord.targetLang,
                        "quiz_starter"
                );
            }
        }

        if (!correct) {
            QuizResultEntity wrong = new QuizResultEntity();
            wrong.questionType = q.type.name();
            wrong.targetLang = currentTargetCode;
            wrong.question = q.prompt;
            wrong.correctAnswer = q.correctAnswer;
            wrong.userAnswer = chosen;
            wrong.wordLabelEn = q.quizWord.labelEn;
            currentWrongResults.add(wrong);
        }

        cardQuiz.postDelayed(() -> {
            currentIndex++;
            showQuestion();
        }, 950);
    }

    private void finishQuiz() {
        cardQuiz.setVisibility(View.GONE);
        txtEmpty.setVisibility(View.VISIBLE);

        int wrongCount = questions.size() - correctCount;
        txtEmpty.setText("Hoàn thành! Đúng " + correctCount + "/" + questions.size()
                + " | Sai " + wrongCount);

        String email = new UserDatabase(requireContext()).getLoggedInEmail();
        if (email != null) {
            repository.saveQuizSession(
                    currentSessionId,
                    email,
                    currentTargetCode,
                    currentSourceMode,
                    questions.size(),
                    correctCount,
                    earnedPoints,
                    maxPoints
            );
        }

        renderLastQuizSummary();
        loadScore();
        refreshSourceHint();
    }

    private void renderLastQuizSummary() {
        StringBuilder sb = new StringBuilder();
        int wrongCount = questions.size() - correctCount;

        sb.append("Language: ").append(currentTargetCode)
                .append("\nSource: ").append(currentSourceMode)
                .append("\nĐúng: ").append(correctCount)
                .append("\nSai: ").append(wrongCount);

        if (!currentWrongResults.isEmpty()) {
            sb.append("\n\nCâu sai:");
            for (QuizResultEntity r : currentWrongResults) {
                sb.append("\n- ").append(r.questionType)
                        .append(": ").append(r.question)
                        .append("\n  Bạn trả lời: ").append(r.userAnswer)
                        .append("\n  Đúng: ").append(r.correctAnswer);
            }
        } else {
            sb.append("\n\nKhông có câu sai nào.");
        }

        txtLastQuizReview.setText(sb.toString());
    }

    private List<String> buildWrongTranslatedOptions(List<QuizWord> pool, QuizWord correctWord, int count) {
        List<String> wrongs = new ArrayList<>();

        for (QuizWord w : pool) {
            if (w.labelEn.equalsIgnoreCase(correctWord.labelEn)) continue;
            if (w.translated.equalsIgnoreCase(correctWord.translated)) continue;
            if (!wrongs.contains(w.translated)) {
                wrongs.add(w.translated);
            }
        }

        Collections.shuffle(wrongs);
        return wrongs.size() > count ? wrongs.subList(0, count) : wrongs;
    }

    private List<String> buildWrongEnglishOptions(List<QuizWord> pool, QuizWord correctWord, int count) {
        List<String> wrongs = new ArrayList<>();

        for (QuizWord w : pool) {
            if (w.labelEn.equalsIgnoreCase(correctWord.labelEn)) continue;
            if (!wrongs.contains(w.labelEn)) {
                wrongs.add(w.labelEn);
            }
        }

        Collections.shuffle(wrongs);
        return wrongs.size() > count ? wrongs.subList(0, count) : wrongs;
    }

    private String pickWrongTranslated(List<QuizWord> pool, QuizWord correctWord) {
        List<String> wrongs = buildWrongTranslatedOptions(pool, correctWord, 10);
        return wrongs.isEmpty() ? "" : wrongs.get(0);
    }

    private String shuffleLetters(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars);

        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }

    private void highlightMcq(String chosen, String correctAnswer) {
        MaterialButton[] btns = {btnA, btnB, btnC, btnD};

        for (MaterialButton btn : btns) {
            if (btn.getVisibility() != View.VISIBLE) continue;

            btn.setOnClickListener(null);
            btn.setEnabled(false);

            String txt = btn.getText().toString();

            if (txt.equals(correctAnswer)) {
                btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1D9E75")));
                btn.setTextColor(Color.parseColor("#1D9E75"));
            } else if (txt.equals(chosen)) {
                btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#E24B4A")));
                btn.setTextColor(Color.parseColor("#E24B4A"));
            }
        }
    }

    private void resetButtons() {
        MaterialButton[] btns = {btnA, btnB, btnC, btnD};

        for (MaterialButton btn : btns) {
            btn.setEnabled(true);
            btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1D9E75")));
            btn.setTextColor(getResources().getColor(android.R.color.tab_indicator_text, null));
            btn.setOnClickListener(null);
        }
    }

    private void mergeUniqueByEnglish(List<QuizWord> base, List<QuizWord> extra) {
        LinkedHashMap<String, QuizWord> map = new LinkedHashMap<>();

        for (QuizWord word : base) {
            map.put(word.labelEn.toLowerCase(Locale.ROOT), word);
        }

        for (QuizWord word : extra) {
            String key = word.labelEn.toLowerCase(Locale.ROOT);
            if (!map.containsKey(key)) {
                map.put(key, word);
            }
        }

        base.clear();
        base.addAll(map.values());
    }

    private boolean isAnswerCorrect(String chosen, String expected) {
        String a = normalize(chosen);
        String b = normalize(expected);
        if (a.equals(b)) return true;

        int distance = levenshtein(a, b);
        return b.length() >= 5 && distance <= 1;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    private String safeTranslated(LearnedWordEntity word, String targetLang) {
        if (!isBlank(word.translated)) return word.translated;
        if ("vi".equalsIgnoreCase(targetLang) && !isBlank(word.labelVi)) return word.labelVi;
        return word.translated;
    }

    private boolean isSameLang(String a, String b) {
        return normalize(a).equals(normalize(b));
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\p{Punct}]", "")
                .replaceAll("\\s+", " ");
    }

    private void toast(String s) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseTts();
    }
}