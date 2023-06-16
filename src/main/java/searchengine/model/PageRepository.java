package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PageRepository extends JpaRepository<Page, Integer> {
    Optional<Page> findById(Page pageId);
    boolean existsByPath(String pageUrl);
    Page findByPath(String path);
    List<Page> findAllBySiteId(int siteId);
    Page findBySiteId(int siteId);
    void deleteAllBySiteId(int id);
}
