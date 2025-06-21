package fr.epita.assistants.ping.data.dto;


import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
class FSEntryResponse {
    private String name;
    private String path;
    private Boolean isDirectory;
}