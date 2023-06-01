package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer> {
    void deleteByName(String name);
    Optional<Page> findById(int pageId);
    Site findByName(String name);
}
