package fr.epita.assistants.ping.data.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {
    private String src;
    private String dst;
}