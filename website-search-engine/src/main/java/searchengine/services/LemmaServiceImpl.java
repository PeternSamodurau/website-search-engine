package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        Site site = page.getSite();
        String text = Jsoup.parse(page.getContent()).text();
        Map<String, Integer> lemmas = collectLemmas(text);

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaString = entry.getKey();
            Integer rank = entry.getValue();

            // Находим или создаем лемму
            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaString, site)
                    .orElseGet(() -> {
                        Lemma newLemma = new Lemma();
                        newLemma.setLemma(lemmaString);
                        newLemma.setSite(site);
                        newLemma.setFrequency(0); // Изначально 0, увеличим ниже
                        return newLemma;
                    });

            // Увеличиваем общую частоту леммы для сайта
            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);

            // Создаем запись в таблице index
            Index index = new Index();
            index.setPage(page);
            index.setLemma(lemma);
            index.setRank(rank.floatValue());
            indexRepository.save(index);
        }
    }

    @Override
    public Map<String, Integer> getLemmasFromQuery(String query, Site site) {
        String text = Jsoup.parse(query).text();
        return collectLemmas(text);
    }

    @Override
    public HashMap<String, Integer> collectLemmas(String textContent) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        String[] words = arrayContainsRussianWords(textContent);

        for (String word : words) {
            if (word.isBlank() || word.length() <= 2) { // Игнорируем короткие слова
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
        // Более строгая проверка, чтобы избежать ложных срабатываний
        return wordBase.matches(".*\\b(ПРЕДЛ|СОЮЗ|МЕЖД|ЧАСТ)\\b.*");
    }

    private String[] arrayContainsRussianWords(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }
}
