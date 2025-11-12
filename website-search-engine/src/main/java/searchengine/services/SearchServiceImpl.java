package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.response.SearchResponseDTO; // Исправлено
import searchengine.dto.response.SearchDataDTO; // Исправлено
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.*;
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

    // Константа для определения "слишком частой" леммы. Если лемма встречается более чем на 90% страниц, она игнорируется.
    private static final double FREQUENCY_THRESHOLD_PERCENT = 0.9;

    @Override
    public SearchResponseDTO search(String query, String siteUrl, int offset, int limit) {
        if (query.isBlank()) {
            return new SearchResponseDTO(false, "Задан пустой поисковый запрос");
        }

        try {
            // 1. Определяем, по каким сайтам будем искать
            List<Site> sitesToSearch = getSitesToSearch(siteUrl);
            if (sitesToSearch.isEmpty()) {
                return new SearchResponseDTO(false, "Сайты для поиска не найдены или не проиндексированы");
            }

            // 2. Получаем уникальные леммы из поискового запроса
            Set<String> queryLemmas = lemmaService.getLemmaSet(query);

            // 3. Ищем релевантные страницы по всем сайтам
            List<SearchDataDTO> allResults = new ArrayList<>(); // Исправлено
            for (Site site : sitesToSearch) {
                allResults.addAll(searchSite(site, query, queryLemmas));
            }

            if (allResults.isEmpty()) {
                return new SearchResponseDTO(true, 0, Collections.emptyList());
            }

            // 4. Рассчитываем относительную релевантность, сортируем и применяем пагинацию
            List<SearchDataDTO> finalResults = postProcessResults(allResults, offset, limit); // Исправлено

            return new SearchResponseDTO(true, allResults.size(), finalResults);

        } catch (Exception e) {
            log.error("Ошибка во время поиска: {}", e.getMessage(), e);
            return new SearchResponseDTO(false, "Во время поиска произошла ошибка: " + e.getMessage());
        }
    }

    /**
     * Определяет список сайтов для поиска.
     * Если siteUrl указан, ищет конкретный сайт. Иначе возвращает все сайты.
     */
    private List<Site> getSitesToSearch(String siteUrl) {
        if (siteUrl != null) {
            return siteRepository.findByUrl(siteUrl).map(List::of).orElse(Collections.emptyList());
        } else {
            return siteRepository.findAll();
        }
    }

    /**
     * Выполняет поиск в пределах одного сайта.
     */
    private List<SearchDataDTO> searchSite(Site site, String query, Set<String> queryLemmas) { // Исправлено
        // 3.1. Находим леммы из запроса в базе данных для данного сайта
        // ПРИМЕЧАНИЕ: Для этого необходим метод в LemmaRepository:
        // List<Lemma> findByLemmaInAndSite(Collection<String> lemmas, Site site);
        List<Lemma> foundLemmas = lemmaRepository.findByLemmaInAndSite(queryLemmas, site);

        // 3.2. Фильтруем слишком частые леммы и сортируем по редкости (от самой редкой к самой частой)
        List<Lemma> filteredAndSortedLemmas = filterAndSortLemmas(foundLemmas, site);
        if (filteredAndSortedLemmas.isEmpty()) {
            return Collections.emptyList();
        }

        // 3.3. Находим страницы, которые содержат ВСЕ отфильтрованные леммы
        List<Integer> lemmaIds = filteredAndSortedLemmas.stream().map(Lemma::getId).toList();
        // ПРИМЕЧАНИЕ: Для этого шага необходим кастомный метод в IndexRepository:
        // @Query(value = "SELECT i.page_id FROM `index` i WHERE i.lemma_id IN :lemmaIds GROUP BY i.page_id HAVING COUNT(DISTINCT i.lemma_id) = :lemmaCount", nativeQuery = true)
        // List<Integer> findPageIdsByLemmaIds(@Param("lemmaIds") List<Integer> lemmaIds, @Param("lemmaCount") int lemmaCount);
        List<Integer> pageIds = indexRepository.findPageIdsByLemmaIds(lemmaIds, lemmaIds.size());

        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3.4. Получаем модели страниц
        List<Page> foundPages = pageRepository.findAllById(pageIds);

        // 3.5. Получаем все `rank` для найденных страниц и лемм одним запросом для расчета релевантности
        // ПРИМЕЧАНИЕ: Для этого шага необходим метод в IndexRepository:
        // List<Index> findByPageInAndLemmaIn(Collection<Page> pages, Collection<Lemma> lemmas);
        List<Index> indexes = indexRepository.findByPageInAndLemmaIn(foundPages, filteredAndSortedLemmas);
        Map<Integer, Float> absoluteRelevanceByPageId = calculateAbsoluteRelevance(indexes);

        // 3.6. Генерируем DTO с результатами
        return createSearchDataDTOs(foundPages, absoluteRelevanceByPageId, query, site); // Исправлено
    }

    /**
     * Фильтрует леммы, которые встречаются слишком часто, и сортирует их по возрастанию частоты.
     */
    private List<Lemma> filterAndSortLemmas(List<Lemma> lemmas, Site site) {
        long totalPagesOnSite = pageRepository.countBySiteId(site.getId());
        long frequencyThreshold = (long) (totalPagesOnSite * FREQUENCY_THRESHOLD_PERCENT);

        return lemmas.stream()
                .filter(lemma -> lemma.getFrequency() <= frequencyThreshold)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
    }

    /**
     * Рассчитывает абсолютную релевантность для каждой страницы (сумма rank'ов).
     */
    private Map<Integer, Float> calculateAbsoluteRelevance(List<Index> indexes) {
        return indexes.stream()
                .collect(Collectors.groupingBy(
                        index -> index.getPage().getId(),
                        Collectors.summingDouble(Index::getRank)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().floatValue()));
    }

    /**
     * Создает список DTO с результатами поиска для одного сайта.
     */
    private List<SearchDataDTO> createSearchDataDTOs(List<Page> pages, Map<Integer, Float> relevanceMap, String query, Site site) { // Исправлено
        List<SearchDataDTO> results = new ArrayList<>(); // Исправлено
        for (Page page : pages) {
            Document doc = Jsoup.parse(page.getContent());
            String title = doc.title();
            float relevance = relevanceMap.getOrDefault(page.getId(), 0.0f);

            // Генерируем сниппет
            String snippet = generateSnippet(doc.text(), query);

            results.add(new SearchDataDTO( // Исправлено
                    site.getUrl(),
                    site.getName(),
                    page.getPath(),
                    title,
                    snippet,
                    relevance // Пока что это абсолютная релевантность
            ));
        }
        return results;
    }


    /**
     * Генерирует сниппет - фрагмент текста с подсвеченными словами из запроса.
     */
    private String generateSnippet(String text, String query) {
        try {
            // Получаем уникальные слова из запроса
            Set<String> queryWords = Arrays.stream(query.toLowerCase().split("\\s+"))
                                           .collect(Collectors.toSet());

            // Находим все вхождения слов запроса в тексте
            List<Integer> occurrences = new ArrayList<>();
            for (String word : queryWords) {
                int index = text.toLowerCase().indexOf(word);
                while (index != -1) {
                    occurrences.add(index);
                    index = text.toLowerCase().indexOf(word, index + 1);
                }
            }
            if (occurrences.isEmpty()) {
                // Если слова не найдены, возвращаем начало текста
                return text.substring(0, Math.min(text.length(), 200)) + "...";
            }

            // Находим "самый насыщенный" фрагмент
            occurrences.sort(Comparator.naturalOrder());
            int bestIndex = 0;
            int maxWords = 0;
            final int fragmentSize = 200; // Желаемый размер фрагмента

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

            // Вырезаем фрагмент, стараясь захватить контекст до и после
            int start = Math.max(0, bestIndex - 50); // Начинаем немного раньше первого вхождения
            int end = Math.min(text.length(), start + fragmentSize + 100); // Заканчиваем немного позже

            String snippet = text.substring(start, end);

            // Подсвечиваем слова
            for (String word : queryWords) {
                // Используем регулярное выражение для поиска слова без учета регистра и обрамляем его тегом <b>
                snippet = snippet.replaceAll("(?i)(" + word + ")", "<b>$1</b>");
            }

            return "..." + snippet + "...";

        } catch (Exception e) {
            log.warn("Не удалось сгенерировать сниппет: {}", e.getMessage());
            // В случае ошибки возвращаем просто начало текста
            return text.substring(0, Math.min(text.length(), 200)) + "...";
        }
    }

    /**
     * Финальная обработка результатов: расчет относительной релевантности, сортировка, пагинация.
     */
    private List<SearchDataDTO> postProcessResults(List<SearchDataDTO> results, int offset, int limit) { // Исправлено
        // Находим максимальную абсолютную релевантность среди всех результатов
        float maxRelevance = results.stream()
                .max(Comparator.comparing(SearchDataDTO::getRelevance)) // Исправлено
                .map(SearchDataDTO::getRelevance) // Исправлено
                .orElse(1.0f); // Если результатов нет, maxRelevance = 1.0f, чтобы избежать деления на ноль

        // Рассчитываем относительную релевантность, сортируем и возвращаем нужный срез
        return results.stream()
                .peek(r -> r.setRelevance(r.getRelevance() / maxRelevance)) // Обновляем до относительной
                .sorted(Comparator.comparing(SearchDataDTO::getRelevance).reversed()) // Исправлено // Сортируем по убыванию релевантности
                .skip(offset) // Применяем смещение для пагинации
                .limit(limit) // Применяем лимит для пагинации
                .collect(Collectors.toList());
    }
}