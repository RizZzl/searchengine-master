package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "site")
@Getter
@Setter
public class SiteDatabase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", nullable = false, unique = true)
    private LocalDateTime statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String name;

    public Optional<SiteDatabase> findById(int siteId) {
        return;
    }

    public void save(SiteDatabase site) {
        id = site.getId();
        status = site.getStatus();
        statusTime = site.getStatusTime();
        lastError = site.getLastError();
        url = site.getUrl();
        name = site.getName();
    }

    public void deleteBySite(String name) {

    }
}
