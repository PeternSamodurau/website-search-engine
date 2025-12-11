package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    int countBySiteId(int siteId);

    @Transactional
    void deleteAllBySite(Site site);

    Optional<Page> findByPathAndSite(String path, Site site);
}
