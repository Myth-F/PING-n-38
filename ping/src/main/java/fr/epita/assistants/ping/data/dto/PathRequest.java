// Generated by delombok at Tue Jun 24 13:03:41 CEST 2025
package fr.epita.assistants.ping.data.dto;

import lombok.*;
import java.util.UUID;

public class PathRequest {
    private String relativePath;

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public String getRelativePath() {
        return this.relativePath;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setRelativePath(final String relativePath) {
        this.relativePath = relativePath;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public PathRequest() {
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public PathRequest(final String relativePath) {
        this.relativePath = relativePath;
    }
}
