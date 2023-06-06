package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByName(String lemma);
    List<Lemma> findAllBySite(Site site);
}
