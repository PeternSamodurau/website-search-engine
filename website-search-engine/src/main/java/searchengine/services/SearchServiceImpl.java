package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponseDTO;
import searchengine.dto.search.SearchDataDTO;
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

    @Value("${search.lemma-frequency-threshold:0.9}")
    private double frequencyThresholdPercent;

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

        List<Lemma> foundLemmas = lemmaRepository.findByLemmaInAndSite(queryLemmas, site);
        log.info("Найдено {} лемм в базе для сайта {}: {}", foundLemmas.size(), site.getName(), foundLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));

        List<Lemma> filteredAndSortedLemmas = filterAndSortLemmas(foundLemmas, site);
        if (filteredAndSortedLemmas.isEmpty()) {
            log.warn("Все леммы были отфильтрованы (слишком частые или не найдены).");
            return Collections.emptyList();
        }
        log.info("Отфильтрованные и отсортированные леммы (от редкой к частой): {}", filteredAndSortedLemmas.stream().map(Lemma::getLemma).collect(Collectors.toList()));

        List<Integer> lemmaIds = filteredAndSortedLemmas.stream().map(Lemma::getId).collect(Collectors.toList());
        List<Integer> pageIds = indexRepository.findPageIdsByLemmaIds(lemmaIds, lemmaIds.size());
        log.info("Найдено {} страниц, содержащих все леммы.", pageIds.size());

        if (pageIds.isEmpty()) {
            return Collections.emptyList();
        }

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
        long frequencyThreshold = (long) (totalPagesOnSite * frequencyThresholdPercent);
        log.info("Порог частоты для фильтрации лемм: {}", frequencyThreshold);

        return lemmas.stream()
                .filter(lemma -> lemma.getFrequency() <= frequencyThreshold)
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

    private String generateSnippet(String text, String query) {
        log.debug("generateSnippet: Входной текст: '{}'", text.substring(0, Math.min(text.length(), 100)) + "...");
        log.debug("generateSnippet: Входной запрос: '{}'", query);

        try {
            Set<String> queryLemmas = lemmaService.getLemmaSet(query);
            log.debug("generateSnippet: Леммы из запроса: {}", queryLemmas);

            if (queryLemmas.isEmpty()) {
                log.debug("generateSnippet: Леммы запроса пусты, возвращаем начало текста.");
                return text.substring(0, Math.min(text.length(), 200)) + "...";
            }

            List<Integer> occurrences = new ArrayList<>();
            Pattern wordPattern = Pattern.compile("\\p{L}+", Pattern.UNICODE_CASE);
            Matcher wordMatcher = wordPattern.matcher(text);

            while (wordMatcher.find()) {
                String word = wordMatcher.group();
                Set<String> wordLemmas = lemmaService.getLemmaSet(word.toLowerCase());

                if (!Collections.disjoint(wordLemmas, queryLemmas)) {
                    occurrences.add(wordMatcher.start());
                    log.debug("generateSnippet: Найдено релевантное слово '{}' (лемма: {}) на позиции {}", word, wordLemmas, wordMatcher.start());
                }
            }
            log.debug("generateSnippet: Всего найдено релевантных вхождений: {}", occurrences.size());

            if (occurrences.isEmpty()) {
                log.debug("generateSnippet: Релевантных вхождений лемм не найдено, возвращаем начало текста.");
                return text.substring(0, Math.min(text.length(), 200)) + "...";
            }

            occurrences.sort(Comparator.naturalOrder());
            log.debug("generateSnippet: Отсортированные позиции релевантных вхождений: {}", occurrences);

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
            log.debug("generateSnippet: Лучший фрагмент начинается с bestIndex: {}", bestIndex);

            int start = Math.max(0, bestIndex - 50);
            int end = Math.min(text.length(), start + fragmentSize + 100);
            String snippetText = text.substring(start, end);
            log.debug("generateSnippet: Сформирован фрагмент текста (до подсветки): '{}'", snippetText);

            StringBuilder highlightedSnippetBuilder = new StringBuilder();
            int lastAppendPosition = 0;

            Matcher snippetWordMatcher = wordPattern.matcher(snippetText);

            while (snippetWordMatcher.find()) {

                highlightedSnippetBuilder.append(snippetText.substring(lastAppendPosition, snippetWordMatcher.start()));

                String word = snippetWordMatcher.group();

                Set<String> wordLemmas = lemmaService.getLemmaSet(word.toLowerCase());


                if (!Collections.disjoint(wordLemmas, queryLemmas)) {
                    highlightedSnippetBuilder.append("<b>").append(word).append("</b>");
                    log.debug("generateSnippet: Подсвечено слово '{}' (лемма: {})", word, wordLemmas);
                } else {
                    highlightedSnippetBuilder.append(word);
                }
                lastAppendPosition = snippetWordMatcher.end();
            }

            highlightedSnippetBuilder.append(snippetText.substring(lastAppendPosition));

            String finalSnippet = highlightedSnippetBuilder.toString();
            log.debug("generateSnippet: Окончательный сниппет: '{}'", "..." + finalSnippet + "...");
            return "..." + finalSnippet + "...";

        } catch (Exception e) {
            log.warn("Не удалось сгенерировать сниппет: {}", e.getMessage(), e);
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