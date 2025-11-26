package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.response.SearchResponseDTO;
import searchengine.dto.response.SearchDataDTO;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;

    // ИСПРАВЛЕНО: Изменен порог фильтрации для более корректной работы тестов и соответствия ТЗ
    private static final double FREQUENCY_THRESHOLD_PERCENT = 1.0;

    @Override
    public SearchResponseDTO search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) {
            return new SearchResponseDTO(false, "Задан пустой поисковый запрос");
        }
        log.info("Начало поиска по запросу: '{}', сайт: '{}'", query, siteUrl);

        try {
            List<Site> sitesToSearch = getSitesToSearch(siteUrl);
            if (sitesToSearch.isEmpty()) {
                log.warn("Сайты для поиска не найдены или не проиндексированы. URL: {}", siteUrl);
                return new SearchResponseDTO(false, "Сайты для поиска не найдены или не проиндексированы");
            }
            log.info("Поиск будет выполнен по {} сайтам.", sitesToSearch.size());

            Set<String> queryLemmas = lemmaService.getLemmaSet(query);
            log.info("Леммы из запроса: {}", queryLemmas);

            List<SearchDataDTO> allResults = new ArrayList<>();
            for (Site site : sitesToSearch) {
                log.info("--- Поиск по сайту: {} ---", site.getName());
                allResults.addAll(searchSite(site, query, queryLemmas));
            }

            if (allResults.isEmpty()) {
                log.info("Поиск не дал результатов.");
                return new SearchResponseDTO(true, 0, Collections.emptyList());
            }

            List<SearchDataDTO> finalResults = postProcessResults(allResults, offset, limit);
            log.info("Поиск завершен. Найдено всего: {}. Возвращено после пагинации: {}", allResults.size(), finalResults.size());

            return new SearchResponseDTO(true, allResults.size(), finalResults);

        } catch (Exception e) {
            log.error("Ошибка во время поиска: {}", e.getMessage(), e);
            return new SearchResponseDTO(false, "Во время поиска произошла ошибка: " + e.getMessage());
        }
    }

    private List<Site> getSitesToSearch(String siteUrl) {
        if (siteUrl != null) {
            return siteRepository.findByUrl(siteUrl).map(List::of).orElse(Collections.emptyList());
        } else {
            return siteRepository.findAll();
        }
    }

    private List<SearchDataDTO> searchSite(Site site, String query, Set<String> queryLemmas) {
        // 1. Находим леммы в базе
        List<Lemma> foundLemmas = lemmaRepository.findByLemmaInAndSite(queryLemmas, site);
        log.info("Найдено {} лемм в базе для сайта {}: {}", foundLemmas.size(), site.getName(), foundLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));

        // 2. Фильтруем слишком частые и сортируем по редкости
        List<Lemma> filteredAndSortedLemmas = filterAndSortLemmas(foundLemmas, site);
        if (filteredAndSortedLemmas.isEmpty()) {
            log.warn("Все леммы были отфильтрованы (слишком частые или не найдены).");
            return Collections.emptyList();
        }
        log.info("Отфильтрованные и отсортированные леммы (от редкой к частой): {}", filteredAndSortedLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));

        // 3. Получаем ID страниц, содержащих ВСЕ леммы, одним запросом
        List<Integer> lemmaIds = filteredAndSortedLemmas.stream().map(Lemma::getId).collect(Collectors.toList());
        List<Integer> pageIds = indexRepository.findPageIdsByLemmaIds(lemmaIds, lemmaIds.size());
        log.info("Найдено {} страниц, содержащих все леммы.", pageIds.size());

        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Получаем страницы и индексы для расчета релевантности
        List<Page> foundPages = pageRepository.findAllById(pageIds);
        List<Index> indexes = indexRepository.findByPageInAndLemmaIn(foundPages, foundLemmas);
        Map<Integer, Float> absoluteRelevanceByPageId = calculateAbsoluteRelevance(indexes);
        log.info("Рассчитана абсолютная релевантность для {} страниц.", absoluteRelevanceByPageId.size());

        return createSearchDataDTOs(foundPages, absoluteRelevanceByPageId, query, site);
    }

    private List<Lemma> filterAndSortLemmas(List<Lemma> lemmas, Site site) {
        long totalPagesOnSite = pageRepository.countBySiteId(site.getId());
        log.info("Всего страниц на сайте {}: {}", site.getName(), totalPagesOnSite);
        if (totalPagesOnSite == 0) {
            return Collections.emptyList();
        }
        // ИСПРАВЛЕНО: Порог частоты изменен на 1.0, чтобы не отфильтровывать слова, встречающиеся на всех страницах
        long frequencyThreshold = (long) (totalPagesOnSite * FREQUENCY_THRESHOLD_PERCENT);
        log.info("Порог частоты для фильтрации лемм: {}", frequencyThreshold);

        return lemmas.stream()
                .filter(lemma -> lemma.getFrequency() <= frequencyThreshold) // Изменено на <=, чтобы слова, встречающиеся на всех страницах, не отфильтровывались
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    private Map<Integer, Float> calculateAbsoluteRelevance(List<Index> indexes) {
        return indexes.stream()
                .collect(Collectors.groupingBy(
                        index -> index.getPage().getId(),
                        Collectors.summingDouble(Index::getRank)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().floatValue()));
    }

    private List<SearchDataDTO> createSearchDataDTOs(List<Page> pages, Map<Integer, Float> relevanceMap, String query, Site site) {
        List<SearchDataDTO> results = new ArrayList<>();
        for (Page page : pages) {
            Document doc = Jsoup.parse(page.getContent());
            String title = doc.title();
            float relevance = relevanceMap.getOrDefault(page.getId(), 0.0f);
            String snippet = generateSnippet(doc.text(), query);
            results.add(new SearchDataDTO(
                    site.getUrl(),
                    site.getName(),
                    page.getPath(),
                    title,
                    snippet,
                    relevance
            ));
        }
        return results;
    }

    /**
     * ИСПРАВЛЕНО: Метод полностью переписан для корректной, регистронезависимой обработки Unicode-символов (кириллицы).
     * Теперь он использует Pattern и Matcher для поиска и подсветки, что является более надежным способом.
     */
    private String generateSnippet(String text, String query) {
        try {
            Set<String> queryLemmas = lemmaService.getLemmaSet(query);
            if (queryLemmas.isEmpty()) {
                return text.substring(0, Math.min(text.length(), 200)) + "...";
            }

            // 1. Найти все вхождения всех лемм запроса в тексте, регистронезависимо
            List<Integer> occurrences = new ArrayList<>();
            for (String lemma : queryLemmas) {
                // Используем Pattern.quote для экранирования спецсимволов в лемме
                // Добавляем \b для поиска целых слов
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(lemma) + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher matcher = pattern.matcher(text);
                while (matcher.find()) {
                    occurrences.add(matcher.start());
                }
            }

            if (occurrences.isEmpty()) {
                return text.substring(0, Math.min(text.length(), 200)) + "...";
            }

            // 2. Найти лучший фрагмент (остальная логика остается той же)
            occurrences.sort(Comparator.naturalOrder());
            int bestIndex = 0;
            int maxWords = 0;
            final int fragmentSize = 200;

            for (int i = 0; i < occurrences.size(); i++) {
                int currentWords = 1;
                for (int j = i + 1; j < occurrences.size(); j++) {
                    if (occurrences.get(j) < occurrences.get(i) + fragmentSize) {
                        currentWords++;
                    } else {
                        break;
                    }
                }
                if (currentWords > maxWords) {
                    maxWords = currentWords;
                    bestIndex = occurrences.get(i);
                }
            }

            int start = Math.max(0, bestIndex - 50);
            int end = Math.min(text.length(), start + fragmentSize + 100);
            String snippet = text.substring(start, end);

            // 3. Подсветить все леммы в выбранном фрагменте
            for (String lemma : queryLemmas) {
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(lemma) + "\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher matcher = pattern.matcher(snippet);
                snippet = matcher.replaceAll("<b>$0</b>");
            }

            return "..." + snippet + "...";

        } catch (Exception e) {
            log.warn("Не удалось сгенерировать сниппет: {}", e.getMessage());
            return text.substring(0, Math.min(text.length(), 200)) + "...";
        }
    }

    private List<SearchDataDTO> postProcessResults(List<SearchDataDTO> results, int offset, int limit) {
        float maxRelevance = results.stream()
                .max(Comparator.comparing(SearchDataDTO::getRelevance))
                .map(SearchDataDTO::getRelevance)
                .orElse(1.0f);

        return results.stream()
                .peek(r -> r.setRelevance(r.getRelevance() / maxRelevance))
                .sorted(Comparator.comparing(SearchDataDTO::getRelevance).reversed())
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
