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
    private PageDatabase page;

    @ManyToOne
    private LemmaDatabase lemma;

    @Column(nullable = false, unique = true)
    private float rank;
}
