package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    // This method is no longer needed for the upsert logic, but keeping it for now if it's used elsewhere.
    // If not used, it can be removed.
    Optional<Lemma> findWithLockByLemmaAndSite(String lemma, Site site);

    @Modifying
    @Query(value = "INSERT INTO lemma (lemma, site_id, frequency) VALUES (:lemma, :siteId, 1) " +
            "ON CONFLICT (lemma, site_id) DO UPDATE SET frequency = lemma.frequency + 1", // Removed RETURNING *
            nativeQuery = true)
    int upsertLemma(@Param("lemma") String lemma, @Param("siteId") int siteId); // Changed return type to int


    @Transactional
    void deleteAllBySite(Site site);
}