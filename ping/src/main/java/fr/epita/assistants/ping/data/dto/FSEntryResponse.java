// Generated by delombok at Tue Jun 24 13:03:41 CEST 2025
package fr.epita.assistants.ping.data.dto;

import lombok.*;

public class FSEntryResponse {
    private String name;
    private String path;
    private Boolean isDirectory;

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public String getName() {
        return this.name;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public String getPath() {
        return this.path;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public Boolean getIsDirectory() {
        return this.isDirectory;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setName(final String name) {
        this.name = name;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setPath(final String path) {
        this.path = path;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setIsDirectory(final Boolean isDirectory) {
        this.isDirectory = isDirectory;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public FSEntryResponse() {
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public FSEntryResponse(final String name, final String path, final Boolean isDirectory) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
    }
}
