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
        // 1. Удаляем старые данные для этой страницы, чтобы обеспечить корректный подсчет частоты.
        deleteDataForPage(page);

        // 2. Парсим и собираем новые леммы.
        Document doc = Jsoup.parse(page.getContent());
        String textForLemmas = doc.title() + " " + doc.body().text();

        Map<String, Integer> lemmasFromPage = collectLemmas(textForLemmas);

        if (lemmasFromPage.isEmpty()) {
            log.warn("Для страницы {} не найдено подходящих лемм.", page.getPath());
            return;
        }

        // 3. Выполняем upsert для всех лемм, чтобы обновить их частоту или создать новые.

        for (String lemmaString : lemmasFromPage.keySet()) {
            lemmaRepository.upsertLemmaFrequency(lemmaString, page.getSite().getId());
        }

        // 4. Получаем актуальные объекты Lemma из базы данных для создания Index.
        List<Lemma> updatedLemmas = lemmaRepository.findByLemmaInAndSite(lemmasFromPage.keySet(), page.getSite());
        Map<String, Lemma> lemmaMap = updatedLemmas.stream()
                .collect(Collectors.toMap(Lemma::getLemma, lemma -> lemma));

        List<Index> indicesToSave = new ArrayList<>();

        // 5. Создаем объекты Index, используя актуальные Lemma.
        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rankOnPage = lemmaEntry.getValue();

            Lemma lemma = lemmaMap.get(lemmaString);

            if (lemma == null) {
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

        // 6. Сохраняем все новые индексы.
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

        // 1. Собираем уникальный набор лемм, связанных с удаляемой страницей.
        Set<Lemma> uniqueLemmas = oldIndices.stream()
                .map(Index::getLemma)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. Удаляем все старые записи Index для данной страницы.
        indexRepository.deleteAllByPage(page);

        List<Lemma> lemmasToUpdate = new ArrayList<>();
        List<Lemma> lemmasToDelete = new ArrayList<>();

        // 3. Для каждой уникальной леммы уменьшаем ее частоту на 1.
        for (Lemma lemma : uniqueLemmas) {
            lemma.setFrequency(lemma.getFrequency() - 1);
            if (lemma.getFrequency() <= 0) {
                lemmasToDelete.add(lemma);
            } else {
                lemmasToUpdate.add(lemma);
            }
        }

        // 4. Сохраняем обновленные леммы и удаляем те, чья частота стала нулевой.
        if (!lemmasToUpdate.isEmpty()) {
            lemmaRepository.saveAll(lemmasToUpdate);
        }
        if (!lemmasToDelete.isEmpty()) {
            lemmaRepository.deleteAll(lemmasToDelete);
        }

        // 5. Очищаем контекст персистентности для предотвращения неожиданного поведения.
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