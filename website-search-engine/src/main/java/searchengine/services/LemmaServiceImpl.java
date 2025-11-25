package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    public void lemmatizePage(Page page) {
        // ЭТОТ МЕТОД ОСТАЕТСЯ БЕЗ ИЗМЕНЕНИЙ - ОН УЖЕ РАБОТАЕТ ПРАВИЛЬНО
        String content = Jsoup.parse(page.getContent()).text();
        Map<String, Integer> lemmas = collectLemmas(content);

        for (Map.Entry<String, Integer> lemmaEntry : lemmas.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rank = lemmaEntry.getValue();

            lemmaRepository.findByLemmaAndSite(lemmaString, page.getSite()).ifPresentOrElse(
                    lemma -> {
                        lemma.setFrequency(lemma.getFrequency() + 1);
                        lemmaRepository.save(lemma);

                        indexRepository.findByLemmaAndPage(lemma, page).ifPresentOrElse(
                                existingIndex -> {
                                    existingIndex.setRank(existingIndex.getRank() + rank.floatValue());
                                    indexRepository.save(existingIndex);
                                },
                                () -> {
                                    Index newIndex = new Index();
                                    newIndex.setLemma(lemma);
                                    newIndex.setPage(page);
                                    newIndex.setRank(rank.floatValue());
                                    indexRepository.save(newIndex);
                                }
                        );
                    },
                    () -> {
                        Lemma newLemma = new Lemma();
                        newLemma.setLemma(lemmaString);
                        newLemma.setSite(page.getSite());
                        newLemma.setFrequency(1);
                        lemmaRepository.save(newLemma);

                        Index newIndex = new Index();
                        newIndex.setLemma(newLemma);
                        newIndex.setPage(page);
                        newIndex.setRank(rank.floatValue());
                        indexRepository.save(newIndex);
                    }
            );
        }
    }

    @Override
    public Set<String> getLemmaSet(String text) {
        return collectLemmas(text).keySet();
    }

    // --- ИЗМЕНЕНИЯ НАЧИНАЮТСЯ ЗДЕСЬ ---

    public Map<String, Integer> collectLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        if (text == null || text.isBlank()) {
            return lemmas;
        }

        try {
            LuceneMorphology luceneMorphologyRu = new RussianLuceneMorphology();
            LuceneMorphology luceneMorphologyEn = new EnglishLuceneMorphology();

            String[] words = splitTextIntoWords(text);

            for (String word : words) {
                if (word.isBlank() || word.length() <= 1) { // Игнорируем слишком короткие слова
                    continue;
                }

                List<String> normalForms;
                LuceneMorphology luceneMorphology;

                if (isRussian(word)) {
                    luceneMorphology = luceneMorphologyRu;
                } else if (isEnglish(word)) {
                    luceneMorphology = luceneMorphologyEn;
                } else {
                    continue; // Пропускаем смешанные или другие слова
                }

                List<String> morphInfo = luceneMorphology.getMorphInfo(word);
                if (isServicePart(morphInfo)) {
                    continue;
                }

                normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }
                String normalWord = normalForms.get(0);
                lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
            }
        } catch (IOException e) {
            log.error("Ошибка при создании морфологического анализатора: {}", e.getMessage());
        }
        return lemmas;
    }

    private String[] splitTextIntoWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^а-яa-z\\s]", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isServicePart(List<String> morphInfo) {
        if (morphInfo.isEmpty()) {
            return false;
        }
        String info = morphInfo.get(0);
        // Части речи в русской и английской морфологии
        return info.contains("ПРЕДЛ") || info.contains("СОЮЗ") || info.contains("МЕЖД") || info.contains("ЧАСТ") // Русский
                || info.contains("PREP") || info.contains("CONJ") || info.contains("PART"); // Английский
    }

    private boolean isRussian(String word) {
        return word.matches("[а-я]+");
    }

    private boolean isEnglish(String word) {
        return word.matches("[a-z]+");
    }
    // --- ИЗМЕНЕНИЯ ЗАКАНЧИВАЮТСЯ ЗДЕСЬ ---
}