package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.HashMap;
import java.util.Map;

public interface LemmaService {
    /**
     * Метод для лемматизации текста страницы и сохранения лемм в базу данных.
     * @param page страница, для которой нужно провести лемматизацию
     */
    void lemmatizePage(Page page);

    /**
     * Метод для получения лемм из поискового запроса.
     * @param query поисковый запрос
     * @param site сайт, в контексте которого выполняется поиск
     * @return Map, где ключ - лемма, а значение - ее частота в запросе
     */
    Map<String, Integer> getLemmasFromQuery(String query, Site site);

    /**
     * Метод для сбора лемм из текстового контента.
     * @param textContent текстовый контент для лемматизации
     * @return HashMap, где ключ - лемма, а значение - ее количество в тексте
     */
    HashMap<String, Integer> collectLemmas(String textContent);
}
