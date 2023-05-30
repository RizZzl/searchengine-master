package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteBySite(String name);
    Optional<Page> findById(int pageId);

    boolean existsByPath(String pageUrl);
}
