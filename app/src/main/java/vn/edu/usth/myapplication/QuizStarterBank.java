package vn.edu.usth.myapplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizStarterBank {

    public static class StarterWord {
        public final String labelEn;
        public final String translated;
        public final String targetLang;

        public StarterWord(String labelEn, String translated, String targetLang) {
            this.labelEn = labelEn;
            this.translated = translated;
            this.targetLang = targetLang;
        }
    }

    public static List<StarterWord> getWords(String targetLang) {
        List<StarterWord> words = new ArrayList<>();

        switch (targetLang) {
            case "vi":
                words.add(new StarterWord("apple", "quả táo", "vi"));
                words.add(new StarterWord("dog", "con chó", "vi"));
                words.add(new StarterWord("cat", "con mèo", "vi"));
                words.add(new StarterWord("car", "xe hơi", "vi"));
                words.add(new StarterWord("book", "quyển sách", "vi"));
                words.add(new StarterWord("chair", "cái ghế", "vi"));
                words.add(new StarterWord("table", "cái bàn", "vi"));
                words.add(new StarterWord("banana", "quả chuối", "vi"));
                words.add(new StarterWord("bird", "con chim", "vi"));
                words.add(new StarterWord("fish", "con cá", "vi"));
                break;

            case "en":
                words.add(new StarterWord("apple", "apple", "en"));
                words.add(new StarterWord("dog", "dog", "en"));
                words.add(new StarterWord("cat", "cat", "en"));
                words.add(new StarterWord("car", "car", "en"));
                words.add(new StarterWord("book", "book", "en"));
                words.add(new StarterWord("chair", "chair", "en"));
                words.add(new StarterWord("table", "table", "en"));
                words.add(new StarterWord("banana", "banana", "en"));
                words.add(new StarterWord("bird", "bird", "en"));
                words.add(new StarterWord("fish", "fish", "en"));
                break;

            case "fr":
                words.add(new StarterWord("apple", "pomme", "fr"));
                words.add(new StarterWord("dog", "chien", "fr"));
                words.add(new StarterWord("cat", "chat", "fr"));
                words.add(new StarterWord("car", "voiture", "fr"));
                words.add(new StarterWord("book", "livre", "fr"));
                words.add(new StarterWord("chair", "chaise", "fr"));
                words.add(new StarterWord("table", "table", "fr"));
                words.add(new StarterWord("banana", "banane", "fr"));
                words.add(new StarterWord("bird", "oiseau", "fr"));
                words.add(new StarterWord("fish", "poisson", "fr"));
                break;

            case "de":
                words.add(new StarterWord("apple", "Apfel", "de"));
                words.add(new StarterWord("dog", "Hund", "de"));
                words.add(new StarterWord("cat", "Katze", "de"));
                words.add(new StarterWord("car", "Auto", "de"));
                words.add(new StarterWord("book", "Buch", "de"));
                words.add(new StarterWord("chair", "Stuhl", "de"));
                words.add(new StarterWord("table", "Tisch", "de"));
                words.add(new StarterWord("banana", "Banane", "de"));
                words.add(new StarterWord("bird", "Vogel", "de"));
                words.add(new StarterWord("fish", "Fisch", "de"));
                break;

            case "es":
                words.add(new StarterWord("apple", "manzana", "es"));
                words.add(new StarterWord("dog", "perro", "es"));
                words.add(new StarterWord("cat", "gato", "es"));
                words.add(new StarterWord("car", "coche", "es"));
                words.add(new StarterWord("book", "libro", "es"));
                words.add(new StarterWord("chair", "silla", "es"));
                words.add(new StarterWord("table", "mesa", "es"));
                words.add(new StarterWord("banana", "plátano", "es"));
                words.add(new StarterWord("bird", "pájaro", "es"));
                words.add(new StarterWord("fish", "pez", "es"));
                break;

            case "ja":
                words.add(new StarterWord("apple", "りんご", "ja"));
                words.add(new StarterWord("dog", "犬", "ja"));
                words.add(new StarterWord("cat", "猫", "ja"));
                words.add(new StarterWord("car", "車", "ja"));
                words.add(new StarterWord("book", "本", "ja"));
                words.add(new StarterWord("chair", "椅子", "ja"));
                words.add(new StarterWord("table", "テーブル", "ja"));
                words.add(new StarterWord("banana", "バナナ", "ja"));
                words.add(new StarterWord("bird", "鳥", "ja"));
                words.add(new StarterWord("fish", "魚", "ja"));
                break;

            case "ko":
                words.add(new StarterWord("apple", "사과", "ko"));
                words.add(new StarterWord("dog", "개", "ko"));
                words.add(new StarterWord("cat", "고양이", "ko"));
                words.add(new StarterWord("car", "자동차", "ko"));
                words.add(new StarterWord("book", "책", "ko"));
                words.add(new StarterWord("chair", "의자", "ko"));
                words.add(new StarterWord("table", "테이블", "ko"));
                words.add(new StarterWord("banana", "바나나", "ko"));
                words.add(new StarterWord("bird", "새", "ko"));
                words.add(new StarterWord("fish", "물고기", "ko"));
                break;

            case "ru":
                words.add(new StarterWord("apple", "яблоко", "ru"));
                words.add(new StarterWord("dog", "собака", "ru"));
                words.add(new StarterWord("cat", "кошка", "ru"));
                words.add(new StarterWord("car", "машина", "ru"));
                words.add(new StarterWord("book", "книга", "ru"));
                words.add(new StarterWord("chair", "стул", "ru"));
                words.add(new StarterWord("table", "стол", "ru"));
                words.add(new StarterWord("banana", "банан", "ru"));
                words.add(new StarterWord("bird", "птица", "ru"));
                words.add(new StarterWord("fish", "рыба", "ru"));
                break;

            case "th":
                words.add(new StarterWord("apple", "แอปเปิล", "th"));
                words.add(new StarterWord("dog", "สุนัข", "th"));
                words.add(new StarterWord("cat", "แมว", "th"));
                words.add(new StarterWord("car", "รถยนต์", "th"));
                words.add(new StarterWord("book", "หนังสือ", "th"));
                words.add(new StarterWord("chair", "เก้าอี้", "th"));
                words.add(new StarterWord("table", "โต๊ะ", "th"));
                words.add(new StarterWord("banana", "กล้วย", "th"));
                words.add(new StarterWord("bird", "นก", "th"));
                words.add(new StarterWord("fish", "ปลา", "th"));
                break;

            case "zh":
                words.add(new StarterWord("apple", "苹果", "zh"));
                words.add(new StarterWord("dog", "狗", "zh"));
                words.add(new StarterWord("cat", "猫", "zh"));
                words.add(new StarterWord("car", "汽车", "zh"));
                words.add(new StarterWord("book", "书", "zh"));
                words.add(new StarterWord("chair", "椅子", "zh"));
                words.add(new StarterWord("table", "桌子", "zh"));
                words.add(new StarterWord("banana", "香蕉", "zh"));
                words.add(new StarterWord("bird", "鸟", "zh"));
                words.add(new StarterWord("fish", "鱼", "zh"));
                break;
        }

        return words;
    }
}