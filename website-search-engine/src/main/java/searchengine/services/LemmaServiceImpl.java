package searchengine.services;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LuceneMorphology russianLuceneMorphology;
    private final LuceneMorphology englishLuceneMorphology;
    private final EntityManager entityManager;

    public LemmaServiceImpl(LemmaRepository lemmaRepository,
                            IndexRepository indexRepository,
                            @Qualifier("russianLuceneMorphology") LuceneMorphology russianLuceneMorphology,
                            @Qualifier("englishLuceneMorphology") LuceneMorphology englishLuceneMorphology,
                            EntityManager entityManager) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.russianLuceneMorphology = russianLuceneMorphology;
        this.englishLuceneMorphology = englishLuceneMorphology;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public synchronized void lemmatizePage(Page page) {
        Document doc = Jsoup.parse(page.getContent());
        String textForLemmas = doc.title() + " " + doc.body().text();

        Map<String, Integer> lemmasFromPage = collectLemmas(textForLemmas);

        if (lemmasFromPage.isEmpty()) {
            log.warn("Для страницы {} не найдено подходящих лемм.", page.getPath());
            return;
        }

        // Fetch existing lemmas from the database in a single query
        List<Lemma> existingLemmasList = lemmaRepository.findByLemmaInAndSite(lemmasFromPage.keySet(), page.getSite());
        Map<String, Lemma> lemmasOnSite = existingLemmasList.stream()
                .collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));

        List<Index> indicesToSave = new ArrayList<>();
        List<Lemma> lemmasToSave = new ArrayList<>();

        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rankOnPage = lemmaEntry.getValue();

            Lemma lemma = lemmasOnSite.get(lemmaString);
            if (lemma == null) {
                // Lemma is new for this site
                lemma = new Lemma();
                lemma.setSite(page.getSite());
                lemma.setLemma(lemmaString);
                lemma.setFrequency(1);
                // Add to map to handle duplicates on the same page
                lemmasOnSite.put(lemmaString, lemma);
                lemmasToSave.add(lemma);
            } else {
                // Existing lemma, just increment frequency
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmasToSave.add(lemma);
            }

            Index newIndex = new Index();
            newIndex.setPage(page);
            newIndex.setLemma(lemma);
            newIndex.setRank(rankOnPage.floatValue());
            indicesToSave.add(newIndex);
        }

        // Batch save all new and updated lemmas.
        // When new lemmas are saved, JPA will assign them an ID.
        lemmaRepository.saveAll(lemmasToSave);

        // Now that all lemmas have been persisted and have an ID,
        // we can safely save the indices that refer to them.
        indexRepository.saveAll(indicesToSave);
    }

    @Override
    @Transactional
    public void deleteDataForPage(Page page) {
        List<Index> oldIndices = indexRepository.findByPage(page);

        if (oldIndices.isEmpty()) {
            return;
        }

        log.debug("Обнаружены старые данные для страницы {}. Начинаю очистку...", page.getPath());
        List<Lemma> lemmasToUpdate = new ArrayList<>();
        List<Lemma> lemmasToDelete = new ArrayList<>();

        for (Index oldIndex : oldIndices) {
            Lemma lemma = oldIndex.getLemma();
            if (lemma == null) {
                continue;
            }
            lemma.setFrequency(lemma.getFrequency() - 1);
            if (lemma.getFrequency() <= 0) {
                lemmasToDelete.add(lemma);
            } else {
                lemmasToUpdate.add(lemma);
            }
        }

        // First, delete the child records
        indexRepository.deleteAll(oldIndices);

        // Then, update/delete the parent records
        lemmaRepository.saveAll(lemmasToUpdate);
        lemmaRepository.deleteAll(lemmasToDelete);

        // Force synchronization with the database and clear the persistence context
        indexRepository.flush();
        entityManager.clear();

        log.debug("Очистка для страницы {} завершена. Контекст персистентности очищен.", page.getPath());
    }


    @Override
    public Set<String> getLemmaSet(String text) {
        return collectLemmas(text).keySet();
    }

    public Map<String, Integer> collectLemmas(String text) {
        Map<String, Integer> lemmas = new HashMap<>();
        if (text == null || text.isBlank()) {
            return lemmas;
        }

        String[] words = splitTextIntoWords(text);

        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }

            List<String> normalForms;
            LuceneMorphology luceneMorphology;

            if (isRussian(word)) {
                luceneMorphology = this.russianLuceneMorphology;
            } else if (isEnglish(word)) {
                luceneMorphology = this.englishLuceneMorphology;
            } else {
                continue;
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
        return info.contains("ПРЕДЛ") || info.contains("СОЮЗ") || info.contains("МЕЖД") || info.contains("ЧАСТ") // Русский
                || info.contains("PREP") || info.contains("CONJ") || info.contains("PART"); // Английский
    }

    private boolean isRussian(String word) {
        return word.matches("[а-я]+");
    }

    private boolean isEnglish(String word) {
        return word.matches("[a-z]+");
    }
}