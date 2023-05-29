package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page")
@Getter
@Setter
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false, unique = true)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false, unique = true)
    private String content;
}
