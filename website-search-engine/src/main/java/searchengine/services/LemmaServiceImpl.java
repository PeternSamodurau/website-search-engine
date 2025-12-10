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

        // 1. Сначала выполняем upsert для всех лемм, чтобы обновить их частоту или создать новые.
        // Это гарантирует, что все леммы существуют в БД с актуальной частотой.
        for (String lemmaString : lemmasFromPage.keySet()) {
            lemmaRepository.upsertLemmaFrequency(lemmaString, page.getSite().getId());
        }
        // Важно: после upsert'а, объекты Lemma в памяти (если они были) не отражают обновленную частоту.
        // Нам нужно получить актуальные объекты Lemma из БД для создания Index.

        // 2. Получаем актуальные объекты Lemma из базы данных.
        // Это нужно, чтобы связать Index с правильными (возможно, обновленными) сущностями Lemma.
        List<Lemma> updatedLemmas = lemmaRepository.findByLemmaInAndSite(lemmasFromPage.keySet(), page.getSite());
        Map<String, Lemma> lemmaMap = updatedLemmas.stream()
                .collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));

        List<Index> indicesToSave = new ArrayList<>();

        // 3. Создаем объекты Index, используя актуальные Lemma.
        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rankOnPage = lemmaEntry.getValue();

            Lemma lemma = lemmaMap.get(lemmaString); // Получаем актуальный объект Lemma

            if (lemma == null) {
                // Этого не должно произойти, если upsert прошел успешно, но для безопасности
                log.error("Лемма '{}' не найдена после upsert для сайта {}. Пропускаю создание индекса.",
                        lemmaString, page.getSite().getName());
                continue;
            }

            Index newIndex = new Index();
            newIndex.setPage(page);
            newIndex.setLemma(lemma);
            newIndex.setRank(rankOnPage.floatValue());
            indicesToSave.add(newIndex);
        }

        // 4. Сохраняем все индексы.
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

        indexRepository.deleteAllByPage(page);
        lemmaRepository.saveAll(lemmasToUpdate);
        lemmaRepository.deleteAll(lemmasToDelete);

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
            // Проверка на минимальную длину слова
            if (word.length() < 2) { // Отфильтровываем слова длиной менее 2 символов
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
        return info.contains("ПРЕДЛ") || info.contains("СОЮЗ") || info.contains("МЕЖД") || info.contains("ЧАСТ") || info.contains("МЕСТОИМ") // Русский
                || info.contains("PREP") || info.contains("CONJ") || info.contains("PART") || info.contains("PN"); // Английский
    }

    private boolean isRussian(String word) {
        return word.matches("[а-я]+");
    }

    private boolean isEnglish(String word) {
        return word.matches("[a-z]+");
    }
}