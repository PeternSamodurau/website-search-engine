package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaServiceImpl implements LemmaService {

    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    @Override
    @Transactional
    public void lemmatizePage(Page page) {
        List<Index> oldIndices = indexRepository.findByPage(page);

        if (!oldIndices.isEmpty()) {
            log.debug("Обнаружены старые данные для страницы {}. Начинаю очистку...", page.getPath());
            for (Index oldIndex : oldIndices) {
                Lemma lemma = oldIndex.getLemma();
                lemma.setFrequency(lemma.getFrequency() - 1);
                if (lemma.getFrequency() == 0) {
                    lemmaRepository.delete(lemma);
                } else {
                    lemmaRepository.save(lemma);
                }
            }
            indexRepository.deleteAll(oldIndices);
        }

        Document doc = Jsoup.parse(page.getContent());
        Elements paragraphs = doc.select("p"); // Выбираем только теги <p>

        StringBuilder textBuilder = new StringBuilder();
        for (Element p : paragraphs) {
            textBuilder.append(p.text()).append(" "); // Извлекаем текст из каждого <p>
        }
        String textForLemmas = textBuilder.toString().trim();


        Map<String, Integer> lemmasFromPage = collectLemmas(textForLemmas);

        if (lemmasFromPage.isEmpty()) {
            log.warn("Для страницы {} не найдено подходящих лемм.", page.getPath());
            return;
        }

        for (Map.Entry<String, Integer> lemmaEntry : lemmasFromPage.entrySet()) {
            String lemmaString = lemmaEntry.getKey();
            Integer rankOnPage = lemmaEntry.getValue();

            Lemma lemma = lemmaRepository.findByLemmaAndSite(lemmaString, page.getSite())
                    .orElseGet(() -> {
                        Lemma newLemma = new Lemma();
                        newLemma.setLemma(lemmaString);
                        newLemma.setSite(page.getSite());
                        newLemma.setFrequency(0);
                        return newLemma;
                    });

            lemma.setFrequency(lemma.getFrequency() + 1);
            lemmaRepository.save(lemma);

            Index newIndex = new Index();
            newIndex.setPage(page);
            newIndex.setLemma(lemma);
            newIndex.setRank(rankOnPage.floatValue());
            indexRepository.save(newIndex);
        }
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

        try {
            LuceneMorphology luceneMorphologyRu = new RussianLuceneMorphology();
            LuceneMorphology luceneMorphologyEn = new EnglishLuceneMorphology();

            String[] words = splitTextIntoWords(text);

            for (String word : words) {
                if (word.isBlank() || word.length() <= 1) {
                    continue;
                }

                List<String> normalForms;
                LuceneMorphology luceneMorphology;

                if (isRussian(word)) {
                    luceneMorphology = luceneMorphologyRu;
                } else if (isEnglish(word)) {
                    luceneMorphology = luceneMorphologyEn;
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
        } catch (IOException e) {
            log.error("Ошибка при создании морфологического анализатора: {}", e.getMessage());
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