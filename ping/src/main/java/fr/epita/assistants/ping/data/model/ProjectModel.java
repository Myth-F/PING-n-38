package fr.epita.assistants.ping.data.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class ProjectModel extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false, referencedColumnName = "id")
    private UserModel owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_members",
        joinColumns = @JoinColumn(name = "project_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<UserModel> members = new ArrayList<>(); //pour la moulinette
    
    @PrePersist
    public void prePersist() {
        if (this.path == null && this.id != null) {
            this.path = "/tmp/ping/projects/" + this.id;
        } else if (this.path == null) {
            this.path = "/tmp/ping/projects/temp-" + UUID.randomUUID();
        }
    }
}
