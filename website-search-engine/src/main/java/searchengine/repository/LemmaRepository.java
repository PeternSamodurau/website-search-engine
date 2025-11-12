package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    // Метод для поиска лемм по списку строк и сайту
    List<Lemma> findByLemmaInAndSite(Collection<String> lemmas, Site site);

    // ИСПРАВЛЕНО: Заменен на явный нативный запрос по ID для надежности и единообразия
    @Query(value = "SELECT count(*) FROM lemma WHERE site_id = ?1", nativeQuery = true)
    int countBySiteId(Integer siteId);

    void deleteAllBySite(Site site);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (?1, ?2, ?3) " +
            "ON CONFLICT (lemma, site_id) DO UPDATE SET frequency = lemma.frequency + EXCLUDED.frequency", nativeQuery = true)
    void upsertLemma(String lemma, Integer siteId, Integer value);
}
