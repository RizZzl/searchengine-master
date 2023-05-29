package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "index")
@Getter
@Setter
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private Page page;

    @ManyToOne
    private Lemma lemma;

    @Column(nullable = false, unique = true)
    private float rank;
}
