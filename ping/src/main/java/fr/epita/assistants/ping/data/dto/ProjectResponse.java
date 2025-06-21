package fr.epita.assistants.ping.data.dto;
import lombok.*;
import java.util.UUID;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProjectResponse {
    private UUID id;
    private String name;
    private java.util.List<UserSummaryResponse> members; //might or might not work, did not test :/
    private UserSummaryResponse owner;
}

