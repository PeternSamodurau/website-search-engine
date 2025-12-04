package searchengine.services;

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

    public LemmaServiceImpl(LemmaRepository lemmaRepository,
                            IndexRepository indexRepository,
                            @Qualifier("russianLuceneMorphology") LuceneMorphology russianLuceneMorphology,
                            @Qualifier("englishLuceneMorphology") LuceneMorphology englishLuceneMorphology) {
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.russianLuceneMorphology = russianLuceneMorphology;
        this.englishLuceneMorphology = englishLuceneMorphology;
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

        Set<String> lemmaStringsOnPage = lemmasFromPage.keySet();

        // Fetch existing lemmas from the database in a single query
        List<Lemma> existingLemmas = lemmaRepository.findByLemmaInAndSite(lemmaStringsOnPage, page.getSite());
        Map<String, Lemma> existingLemmasMap = existingLemmas.stream()
                .collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));

        List<Lemma> lemmasToUpdate = new ArrayList<>(); // For existing lemmas whose frequency needs incrementing
        List<String> newLemmaStrings = new ArrayList<>(); // For lemmas that are new to the DB

        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();

            if (existingLemmasMap.containsKey(lemmaString)) {
                // Lemma already exists, update its frequency in memory
                Lemma lemma = existingLemmasMap.get(lemmaString);
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmasToUpdate.add(lemma); // Add to list for batch update
            } else {
                // New lemma, add to list for upsert
                newLemmaStrings.add(lemmaString);
            }
        }

        // Perform batch updates for existing lemmas
        if (!lemmasToUpdate.isEmpty()) {
            lemmaRepository.saveAll(lemmasToUpdate);
        }

        // Perform upserts for new lemmas
        for (String newLemmaString : newLemmaStrings) {
            lemmaRepository.upsertLemmaFrequency(newLemmaString, page.getSite().getId());
        }

        // After all lemmas are either updated or upserted, re-fetch them to get their current IDs and frequencies
        // This is crucial for correctly linking them to Index entities.
        List<Lemma> allProcessedLemmas = lemmaRepository.findByLemmaInAndSite(lemmaStringsOnPage, page.getSite());
        Map<String, Lemma> allProcessedLemmasMap = allProcessedLemmas.stream()
                .collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));

        List<Index> indicesToSave = new ArrayList<>();
        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rankOnPage = lemmaEntry.getValue();

            Lemma lemma = allProcessedLemmasMap.get(lemmaString);
            if (lemma == null) {
                log.error("Лемма {} не найдена после обработки. Это не должно произойти.", lemmaString);
                continue;
            }

            Index newIndex = new Index();
            newIndex.setPage(page);
            newIndex.setLemma(lemma);
            newIndex.setRank(rankOnPage.floatValue());
            indicesToSave.add(newIndex);
        }

        // Batch save indices
        if (!indicesToSave.isEmpty()) {
            indexRepository.saveAll(indicesToSave);
        }
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
            lemma.setFrequency(lemma.getFrequency() - 1);
            if (lemma.getFrequency() == 0) {
                lemmasToDelete.add(lemma);
            } else {
                lemmasToUpdate.add(lemma);
            }
        }

        lemmaRepository.saveAll(lemmasToUpdate);
        lemmaRepository.deleteAll(lemmasToDelete);
        indexRepository.deleteAll(oldIndices);
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