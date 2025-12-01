package searchengine.services;

import searchengine.model.Page;

import java.util.Set;

public interface LemmaService {
    /**
     * Получает набор уникальных лемм из текста.
     * @param text исходный текст
     * @return набор (Set) уникальных лемм
     */
    Set<String> getLemmaSet(String text);

    /**
     * Выполняет лемматизацию контента страницы и сохраняет леммы и индексы в базу данных.
     * @param page страница, которую нужно лемматизировать
     */
    void lemmatizePage(Page page);

    /**
     * Удаляет все данные (индексы, леммы) связанные с конкретной страницей.
     * @param page страница, данные которой нужно удалить
     */
    void deleteDataForPage(Page page);
}
