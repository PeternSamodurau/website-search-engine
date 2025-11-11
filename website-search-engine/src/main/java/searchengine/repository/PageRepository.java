package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    int countBySiteId(int siteId);

    @Transactional
    void deleteAllBySite(Site site);

    @Query("SELECT p FROM Page p JOIN Index i ON p.id = i.page.id WHERE i.lemma.id = :lemmaId")
    List<Page> findByLemma(@Param("lemmaId") int lemmaId);

    Optional<Page> findByPathAndSite(String path, Site site);
}
