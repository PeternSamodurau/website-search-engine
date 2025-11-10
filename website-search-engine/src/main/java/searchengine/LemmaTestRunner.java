package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LemmaTestRunner {

    public static void main(String[] args) {
        try {
            String text = "Повторное появление леопарда в Осетии позволяет предположить, " +
                          "что леопард постоянно обитает в некоторых районах Северного Кавказа.";

            System.out.println("Исходный текст:");
            System.out.println(text);
            System.out.println("--------------------");

            HashMap<String, Integer> lemmas = collectLemmas(text);

            System.out.println("Результат лемматизации:");
            lemmas.forEach((lemma, count) -> System.out.println(lemma + " — " + count));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод для сбора лемм из текста.
     * @param text исходный текст
     * @return HashMap, где ключ - лемма, значение - количество вхождений
     */
    public static HashMap<String, Integer> collectLemmas(String text) throws IOException {
        HashMap<String, Integer> lemmaMap = new HashMap<>();

        // Создаем экземпляр лемматизатора для русского языка
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();

        // Очищаем текст от знаков препинания и приводим к нижнему регистру
        String cleanText = text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim();

        // Разбиваем текст на слова
        String[] words = cleanText.split("\\s+");

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            // Получаем морфологическую информацию о слове
            List<String> morphInfo = luceneMorph.getMorphInfo(word);
            System.out.println(word + " -> " + morphInfo);

            // Проверяем, не является ли слово служебной частью речи
            if (isServicePart(morphInfo.get(0))) {
                continue;
            }

            // Получаем нормальные формы слова
            List<String> normalForms = luceneMorph.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);

            // Считаем количество
            lemmaMap.put(normalWord, lemmaMap.getOrDefault(normalWord, 0) + 1);
        }

        return lemmaMap;
    }

    /**
     * Проверяет, является ли слово служебной частью речи.
     * @param morphInfo информация о слове от лемматизатора
     * @return true, если слово - служебная часть речи
     */
    private static boolean isServicePart(String morphInfo) {
        // Частицы, предлоги, союзы, междометия
        List<String> serviceParts = Arrays.asList("ЧАСТ", "ПРЕДЛ", "СОЮЗ", "МЕЖД");
        for (String part : serviceParts) {
            if (morphInfo.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
