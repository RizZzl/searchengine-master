package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public interface IndexRepository extends JpaRepository<Index, Integer> {
    Index findByLemmaAndPage(Lemma existingLemma, Page page);
    void deleteByPage(Page page);
    Index findByPageAndLemma(Page page, Lemma lemmaObj);
}
