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

    List<Lemma> findBySite(Site site);

    int countBySite(Site site);

    @Modifying
    @Transactional
    @Query("DELETE FROM Lemma l WHERE l.site = :site")
    void deleteAllBySite(Site site);

    List<Lemma> findByLemmaInAndSite(Collection<String> lemmas, Site site);

    @Modifying
    @Transactional
    @Query(value = "MERGE INTO lemma AS l " +
            "USING (VALUES (:lemma, :siteId)) AS s(lemma_val, site_id_val) " +
            "ON (l.lemma = s.lemma_val AND l.site_id = s.site_id_val) " +
            "WHEN MATCHED THEN UPDATE SET l.frequency = l.frequency + 1 " +
            "WHEN NOT MATCHED THEN INSERT (lemma, site_id, frequency) VALUES (:lemma, :siteId, 1)",
            nativeQuery = true)
    void upsertLemmaFrequency(String lemma, Integer siteId);
}