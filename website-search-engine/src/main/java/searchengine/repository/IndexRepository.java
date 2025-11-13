package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Optional<Index> findByLemmaAndPage(Lemma lemma, Page page);
    List<Index> findByPage(Page page);

    /**
     * Находит ID страниц, которые содержат ВСЕ леммы из переданного списка.
     * @param lemmaIds список ID лемм
     * @param lemmaCount количество лемм в списке (для проверки в HAVING)
     * @return список ID страниц
     */
    @Query(value = "SELECT i.page_id FROM `index` i WHERE i.lemma_id IN :lemmaIds GROUP BY i.page_id HAVING COUNT(DISTINCT i.lemma_id) = :lemmaCount", nativeQuery = true)
    List<Integer> findPageIdsByLemmaIds(@Param("lemmaIds") List<Integer> lemmaIds, @Param("lemmaCount") int lemmaCount);

    /**
     * Находит все индексы для заданных страниц и лемм.
     * @param pages коллекция страниц
     * @param lemmas коллекция лемм
     * @return список индексов
     */
    List<Index> findByPageInAndLemmaIn(Collection<Page> pages, Collection<Lemma> lemmas);

    /**
     * ИСПРАВЛЕНО: Добавлен метод для удаления всех индексов, связанных с конкретной страницей.
     * @param page страница, для которой нужно удалить индексы
     */
    @Transactional
    @Modifying
    void deleteByPage(Page page);
}
