package fr.epita.assistants.ping.data.dto;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String login;
    private String displayName;
    private Boolean isAdmin;
    private String avatar;
}