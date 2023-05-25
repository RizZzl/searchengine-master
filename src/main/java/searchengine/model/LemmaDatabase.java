package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "lemma")
@Getter
@Setter
public class LemmaDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private SiteDatabase site;

    @Column(nullable = false, unique = true)
    private String lemma;

    @Column(nullable = false, unique = true)
    private int frequency;

}
