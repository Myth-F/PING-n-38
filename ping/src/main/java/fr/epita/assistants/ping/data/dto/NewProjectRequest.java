package fr.epita.assistants.ping.data.dto;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
class NewProjectRequest {
    private String name;
}