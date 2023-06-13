package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByLemma(String lemma);
    List<Lemma> findAllBySite(Site site);
    Lemma findByLemmaAndSiteId(String lemma, int id);
    void deleteBySiteId(int id);
    Lemma findByLemmaAndSite(String lemma, Site site);
}
