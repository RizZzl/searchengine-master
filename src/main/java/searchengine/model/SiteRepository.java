package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface SiteRepository extends JpaRepository<Site, Integer> {
    void deleteByName(String name);
    Site findByName(String name);
    Site findById(int siteId);
    Site findByUrl(String url);
}
