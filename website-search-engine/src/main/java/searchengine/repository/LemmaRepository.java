package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    int countBySiteId(int siteId);
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    @Transactional
    void deleteAllBySite(Site site);
}
