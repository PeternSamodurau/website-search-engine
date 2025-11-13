package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final Map<Integer, Object> siteLocks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public void lemmatizePage(Page page) {
        Site site = page.getSite();
        if (site == null) {
            log.error("У страницы с ID {} отсутствует сайт. Лемматизация невозможна.", page.getId());
            return;
        }

        Object siteLock = siteLocks.computeIfAbsent(site.getId(), k -> new Object());

        synchronized (siteLock) {
            String text = Jsoup.parse(page.getContent()).text();
            Map<String, Integer> lemmasFromText = collectLemmas(text);

            if (lemmasFromText.isEmpty()) {
                return;
            }

            for (Map.Entry<String, Integer> entry : lemmasFromText.entrySet()) {
                String lemmaString = entry.getKey();
                Integer rank = entry.getValue();

                Lemma lemma;
                try {
                    // Пытаемся найти лемму. Если нет - создаем и сохраняем.
                    lemma = lemmaRepository.findByLemmaAndSite(lemmaString, site)
                            .orElseGet(() -> {
                                Lemma newLemma = new Lemma();
                                newLemma.setLemma(lemmaString);
                                newLemma.setSite(site);
                                newLemma.setFrequency(0);
                                return lemmaRepository.save(newLemma);
                            });
                } catch (DataIntegrityViolationException e) {
                    // ИСПРАВЛЕНО: Если другой поток уже создал лемму, ловим ошибку и просто загружаем ее.
                    log.warn("Произошла гонка потоков при создании леммы '{}'. Повторно загружаем.", lemmaString);
                    lemma = lemmaRepository.findByLemmaAndSite(lemmaString, site)
                            .orElseThrow(() -> new IllegalStateException("Не удалось найти лемму после гонки потоков: " + lemmaString));
                }

                // Увеличиваем частоту леммы для сайта
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.save(lemma);

                // Создаем новый индекс
                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(rank.floatValue());
                indexRepository.save(index);
            }
        }
    }

    @Override
    public Set<String> getLemmaSet(String text) {
        String cleanText = Jsoup.parse(text).text();
        Map<String, Integer> lemmas = collectLemmas(cleanText);
        return lemmas.keySet();
    }

    private HashMap<String, Integer> collectLemmas(String textContent) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = arrayContainsRussianWords(textContent);

        for (String word : words) {
            if (word.isBlank() || word.length() <= 2) {
                continue;
            }

            List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
            if (anyWordBaseFormIsService(wordBaseForms)) {
                continue;
            }

            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) {
                continue;
            }

            String normalWord = normalForms.get(0);
            lemmas.put(normalWord, lemmas.getOrDefault(normalWord, 0) + 1);
        }
        return lemmas;
    }

    private boolean anyWordBaseFormIsService(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::isServiceWord);
    }

    private boolean isServiceWord(String wordBase) {
        return wordBase.matches(".*\\b(ПРЕДЛ|СОЮЗ|МЕЖД|ЧАСТ)\\b.*");
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}