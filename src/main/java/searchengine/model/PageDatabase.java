package searchengine.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "page")
@Getter
@Setter
public class PageDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private SiteDatabase site;

    @Column(nullable = false, unique = true)
    private String path;

    @Column(nullable = false, unique = true)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false, unique = true)
    private String content;

    public void save(PageDatabase page) {
        id = page.getId();
        site = page.getSite();
        code = page.getCode();
        content = page.getContent();
    }

    public void deleteBySite(String name) {

    }
}
