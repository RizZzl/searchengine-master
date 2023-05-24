package searchengine.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Date;

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
    private Date statusTime;

    @Column(name = "last_error")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false, unique = true)
    private String name;
}
