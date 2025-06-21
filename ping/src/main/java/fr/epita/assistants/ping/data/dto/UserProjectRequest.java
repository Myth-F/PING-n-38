package fr.epita.assistants.ping.data.dto;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserProjectRequest {
    private UUID userId;
}