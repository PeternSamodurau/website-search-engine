package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.dto.response.SearchDataDTO;
import searchengine.dto.response.SearchResponseDTO;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaService lemmaService;

    @Override
    public SearchResponseDTO search(String query, String siteUrl, int offset, int limit) {
        if (query.trim().isEmpty()) {
            return new SearchResponseDTO(false, "Задан пустой поисковый запрос");
        }

        List<Site> sitesToSearch = siteUrl == null ?
                siteRepository.findAll() :
                siteRepository.findByUrl(siteUrl).map(Collections::singletonList).orElse(Collections.emptyList());

        if (sitesToSearch.isEmpty()) {
            return new SearchResponseDTO(false, "Указанный сайт не найден");
        }

        // Лемматизация запроса для всех сайтов, чтобы получить общие леммы
        // Если запрос специфичен для одного сайта, то леммы будут только для него
        Map<String, Integer> queryLemmasMap = new HashMap<>();
        for (Site site : sitesToSearch) {
            queryLemmasMap.putAll(lemmaService.getLemmasFromQuery(query, site));
        }

        Set<String> queryLemmas = queryLemmasMap.keySet();

        List<Lemma> sortedLemmas = queryLemmas.stream()
                .map(lemmaStr -> {
                    // Ищем леммы для каждого сайта, если siteUrl не указан
                    // Если siteUrl указан, то ищем только для этого сайта
                    return sitesToSearch.stream()
                            .map(site -> lemmaRepository.findByLemmaAndSite(lemmaStr, site))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .findFirst(); // Берем первую найденную лемму (для одного сайта это будет единственная)
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());


        if (sortedLemmas.isEmpty()) {
            return new SearchResponseDTO(true, 0, Collections.emptyList());
        }

        List<Page> pages = findPagesWithAllLemmas(sortedLemmas);
        Map<Page, Float> relevanceMap = calculateRelevance(pages, sortedLemmas);

        // Общее количество найденных результатов до пагинации
        int totalCount = relevanceMap.size();

        List<SearchDataDTO> searchData = relevanceMap.entrySet().stream()
                .sorted(Map.Entry.<Page, Float>comparingByValue().reversed())
                .skip(offset)
                .limit(limit)
                .map(entry -> {
                    Page page = entry.getKey();
                    float relevance = entry.getValue();
                    String snippet = generateSnippet(page.getContent(), queryLemmas, page.getSite());
                    Document doc = Jsoup.parse(page.getContent());

                    SearchDataDTO data = new SearchDataDTO();
                    data.setSite(page.getSite().getUrl());
                    data.setSiteName(page.getSite().getName());
                    data.setUri(page.getPath());
                    data.setTitle(doc.title());
                    data.setSnippet(snippet);
                    data.setRelevance(relevance);
                    return data;
                })
                .collect(Collectors.toList());

        return new SearchResponseDTO(true, totalCount, searchData);
    }

    private List<Page> findPagesWithAllLemmas(List<Lemma> lemmas) {
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }
        Lemma firstLemma = lemmas.get(0);
        List<Page> initialPages = pageRepository.findByLemma(firstLemma.getId());

        if (lemmas.size() == 1) {
            return initialPages;
        }

        List<Page> resultPages = new ArrayList<>(initialPages);
        for (int i = 1; i < lemmas.size(); i++) {
            Lemma nextLemma = lemmas.get(i);
            resultPages.retainAll(pageRepository.findByLemma(nextLemma.getId()));
        }
        return resultPages;
    }

    private Map<Page, Float> calculateRelevance(List<Page> pages, List<Lemma> lemmas) {
        Map<Page, Float> relevanceMap = new HashMap<>();
        float maxRelevance = 0;

        for (Page page : pages) {
            float absoluteRelevance = 0;
            for (Lemma lemma : lemmas) {
                Optional<Index> indexOpt = indexRepository.findByLemmaAndPage(lemma, page);
                if (indexOpt.isPresent()) {
                    absoluteRelevance += indexOpt.get().getRank();
                }
            }
            relevanceMap.put(page, absoluteRelevance);
            if (absoluteRelevance > maxRelevance) {
                maxRelevance = absoluteRelevance;
            }
        }

        if (maxRelevance > 0) {
            for (Map.Entry<Page, Float> entry : relevanceMap.entrySet()) {
                entry.setValue(entry.getValue() / maxRelevance);
            }
        }
        return relevanceMap;
    }

    private String generateSnippet(String content, Set<String> queryLemmas, Site site) {
        String text = Jsoup.parse(content).text();
        List<String> words = Arrays.asList(text.split("\\s+"));
        int bestIndex = -1;
        int maxMatches = 0;

        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            if (word.trim().isEmpty()) continue;
            Set<String> wordLemmas = lemmaService.getLemmasFromQuery(word, site).keySet();
            if (!Collections.disjoint(wordLemmas, queryLemmas)) {
                int matches = 0;
                for (int j = i; j < Math.min(i + 20, words.size()); j++) {
                    if (words.get(j).trim().isEmpty()) continue;
                    Set<String> currentWordLemmas = lemmaService.getLemmasFromQuery(words.get(j), site).keySet();
                    if (!Collections.disjoint(currentWordLemmas, queryLemmas)) {
                        matches++;
                    }
                }
                if (matches > maxMatches) {
                    maxMatches = matches;
                    bestIndex = i;
                }
            }
        }

        if (bestIndex == -1) {
            // Если ни одно слово запроса не найдено, возвращаем начало текста
            return text.substring(0, Math.min(text.length(), 200));
        }

        // Определяем границы сниппета
        int start = Math.max(0, bestIndex - 10); // 10 слов до первого совпадения
        int end = Math.min(words.size(), bestIndex + 20); // 20 слов после первого совпадения

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            String word = words.get(i);
            if (word.trim().isEmpty()) continue;
            Set<String> currentWordLemmas = lemmaService.getLemmasFromQuery(word, site).keySet();
            if (!Collections.disjoint(currentWordLemmas, queryLemmas)) {
                snippet.append("<b>").append(word).append("</b>");
            } else {
                snippet.append(word);
            }
            snippet.append(" ");
        }
        return snippet.toString().trim();
    }
}
