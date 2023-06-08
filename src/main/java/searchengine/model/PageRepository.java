package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySite(Site site);
    Optional<Page> findById(int pageId);
    boolean existsByPath(String pageUrl);
    void deleteBySiteId(int siteId);
    Page findByPath(String path);
    List<Page> findAllBySiteId(int id);
}
