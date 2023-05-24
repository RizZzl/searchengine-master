package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "index")
@Getter
@Setter
public class IndexDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "page_id")
    @Column(nullable = false, unique = true)
    private PageDatabase page;

    @ManyToOne
    @JoinColumn(name = "lemma_id")
    @Column(nullable = false, unique = true)
    private LemmaDatabase lemma;

    @Column(nullable = false, unique = true)
    private float rank;
}
