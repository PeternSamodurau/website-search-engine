package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    @Transactional
    public void lemmatizePage(Page page) {
        log.debug("Начало лемматизации страницы: URL='{}', Site='{}'", page.getPath(), page.getSite().getName());
        Site site = page.getSite();
        String text = Jsoup.parse(page.getContent()).text();
        Map<String, Integer> lemmas = collectLemmas(text);

        lemmas.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String lemmaString = entry.getKey();
                    Integer rank = entry.getValue();

                    log.debug("Обработка леммы '{}' с рангом {} для страницы '{}'", lemmaString, rank, page.getPath());

                    Lemma lemma = this.getOrCreateAndIncrementLemma(lemmaString, site, rank);
                    log.debug("Лемма '{}' (ID: {}) получена/создана. Частота обновлена.", lemma.getLemma(), lemma.getId());

                    Optional<Index> optionalIndex = indexRepository.findByLemmaAndPage(lemma, page);
                    if (optionalIndex.isEmpty()) {
                        Index index = new Index();
                        index.setPage(page);
                        index.setLemma(lemma);
                        index.setRank(rank.floatValue());
                        indexRepository.save(index);

                        log.debug("Создан новый индекс для леммы '{}' и страницы '{}'", lemmaString, page.getPath());
                    } else {
                        Index index = optionalIndex.get();
                        index.setRank(index.getRank() + rank.floatValue());
                        indexRepository.save(index);
                        log.debug("Обновлен существующий индекс для леммы '{}' и страницы '{}'. Новый ранг: {}", lemmaString, page.getPath(), index.getRank());
                    }
                });
        log.debug("Завершение лемматизации страницы: URL='{}', Site='{}'", page.getPath(), page.getSite().getName());
    }

    // Метод не является частью интерфейса, но используется внутри класса, оставляем public
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Lemma getOrCreateAndIncrementLemma(String lemmaString, Site site, Integer rank) {
        log.debug("Вызов upsertLemma для леммы '{}' на сайте '{}' со значением {}", lemmaString, site.getName(), rank);
        lemmaRepository.upsertLemma(lemmaString, site.getId(), rank);
        Optional<Lemma> optionalLemma = lemmaRepository.findByLemmaAndSite(lemmaString, site);
        if (optionalLemma.isPresent()) {
            Lemma lemma = optionalLemma.get();
            log.debug("Лемма '{}' (ID: {}) получена после upsert. Текущая частота: {}", lemma.getLemma(), lemma.getId(), lemma.getFrequency());
            return lemma;
        } else {
            log.error("Не удалось найти лемму '{}' для сайта '{}' после успешного upsert.", lemmaString, site.getName());
            throw new IllegalStateException("Lemma not found after upsert operation.");
        }
    }

    @Override
    public Set<String> getLemmaSet(String text) {
        String cleanText = Jsoup.parse(text).text();
        return collectLemmas(cleanText).keySet();
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
