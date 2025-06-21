package fr.epita.assistants.ping.data.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserModel extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String login;

    @Column(nullable = false)
    private String password;

    @Column(name = "display_name")
    private String displayName;

    private String avatar;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @ManyToMany(mappedBy = "members", fetch = FetchType.LAZY)
    private Set<ProjectModel> memberProjects = new HashSet<>();

    @OneToMany(mappedBy = "owner", fetch = FetchType.LAZY)
    private Set<ProjectModel> ownedProjects = new HashSet<>();
}