package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaServiceImpl implements LemmaService {

    private final LuceneMorphology luceneMorphology;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    // private final LemmaServiceImpl self; // Удалено для устранения циклической зависимости

    @Override
    @Transactional
    public void lemmatizePage(Page page) {
        log.debug("Начало лемматизации страницы: URL='{}', Site='{}'", page.getPath(), page.getSite().getName());
        Site site = page.getSite();
        String text = Jsoup.parse(page.getContent()).text();
        Map<String, Integer> lemmas = collectLemmas(text);

        for (Map.Entry<String, Integer> entry : lemmas.entrySet()) {
            String lemmaString = entry.getKey();
            Integer rank = entry.getValue();

            log.debug("Обработка леммы '{}' с рангом {} для страницы '{}'", lemmaString, rank, page.getPath());

            // Получаем или создаем лемму, и сразу увеличиваем ее частоту в отдельной транзакции
            // Этот метод гарантирует, что лемма существует и ее частота обновлена
            Lemma lemma = this.getOrCreateAndIncrementLemma(lemmaString, site); // Изменено с self на this
            log.debug("Лемма '{}' (ID: {}) получена/создана и частота инкрементирована. Частота: {}", lemma.getLemma(), lemma.getId(), lemma.getFrequency());

            // Создаем или обновляем запись в таблице index
            Optional<Index> optionalIndex = indexRepository.findByLemmaAndPage(lemma, page);
            if (optionalIndex.isEmpty()) {
                Index index = new Index();
                index.setPage(page);
                index.setLemma(lemma);
                index.setRank(rank.floatValue());
                indexRepository.save(index);

                log.debug("Создан новый индекс для леммы '{}' и страницы '{}'", lemmaString, page.getPath());
            } else {
                Index index = optionalIndex.get();
                index.setRank(index.getRank() + rank.floatValue()); // Обновляем ранг, если запись уже существует
                indexRepository.save(index);
                log.debug("Обновлен существующий индекс для леммы '{}' и страницы '{}'. Новый ранг: {}", lemmaString, page.getPath(), index.getRank());
            }
        }
        log.debug("Завершение лемматизации страницы: URL='{}', Site='{}'", page.getPath(), page.getSite().getName());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Lemma getOrCreateAndIncrementLemma(String lemmaString, Site site) {
        log.debug("Попытка получить/создать и инкрементировать лемму '{}' для сайта '{}'", lemmaString, site.getName());
        // Пытаемся найти лемму с пессимистической блокировкой
        log.debug("Поиск леммы '{}' с пессимистической блокировкой для сайта '{}'", lemmaString, site.getName());
        Optional<Lemma> optionalLemma = lemmaRepository.findWithLockByLemmaAndSite(lemmaString, site);

        if (optionalLemma.isPresent()) {
            Lemma existingLemma = optionalLemma.get();
            log.debug("Лемма '{}' (ID: {}) найдена. Текущая частота: {}. Инкрементируем.", existingLemma.getLemma(), existingLemma.getId(), existingLemma.getFrequency());
            existingLemma.setFrequency(existingLemma.getFrequency() + 1);
            Lemma updatedLemma = lemmaRepository.save(existingLemma);
            log.debug("Лемма '{}' (ID: {}) обновлена. Новая частота: {}", updatedLemma.getLemma(), updatedLemma.getId(), updatedLemma.getFrequency());
            return updatedLemma;
        } else {
            // Если лемма не найдена, создаем новую с частотой 1
            log.debug("Лемма '{}' не найдена. Создаем новую лемму с частотой 1.", lemmaString);
            Lemma newLemma = new Lemma();
            newLemma.setLemma(lemmaString);
            newLemma.setSite(site);
            newLemma.setFrequency(1); // Изначальная частота 1 при первом создании

            try {
                Lemma savedLemma = lemmaRepository.save(newLemma);
                log.debug("Новая лемма '{}' (ID: {}) успешно создана.", savedLemma.getLemma(), savedLemma.getId());
                return savedLemma;
            } catch (DataIntegrityViolationException e) {
                // Если произошла конкурентная вставка, пытаемся получить уже созданную лемму с блокировкой
                log.warn("Конкурентное создание леммы обнаружено для '{}' на сайте '{}'. Повторное получение с блокировкой.", lemmaString, site.getName());
                return lemmaRepository.findWithLockByLemmaAndSite(lemmaString, site)
                        .map(foundLemma -> {
                            log.debug("После конкурентной вставки лемма '{}' (ID: {}) найдена. Текущая частота: {}. Инкрементируем.", foundLemma.getLemma(), foundLemma.getId(), foundLemma.getFrequency());
                            foundLemma.setFrequency(foundLemma.getFrequency() + 1);
                            Lemma updatedLemma = lemmaRepository.save(foundLemma);
                            log.debug("Лемма '{}' (ID: {}) обновлена после конкурентной вставки. Новая частота: {}", updatedLemma.getLemma(), updatedLemma.getId(), updatedLemma.getFrequency());
                            return updatedLemma;
                        })
                        .orElseThrow(() -> new IllegalStateException("Не удалось получить или создать лемму после конкурентной вставки: " + lemmaString));
            }
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