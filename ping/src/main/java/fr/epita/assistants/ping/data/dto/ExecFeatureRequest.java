// Generated by delombok at Tue Jun 24 13:03:41 CEST 2025
package fr.epita.assistants.ping.data.dto;

import lombok.*;
import java.util.UUID;

public class ExecFeatureRequest {
    private String feature;
    private String command;
    private java.util.List<String> params;

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public String getFeature() {
        return this.feature;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public String getCommand() {
        return this.command;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public java.util.List<String> getParams() {
        return this.params;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setFeature(final String feature) {
        this.feature = feature;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setCommand(final String command) {
        this.command = command;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public void setParams(final java.util.List<String> params) {
        this.params = params;
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public ExecFeatureRequest() {
    }

    @java.lang.SuppressWarnings("all")
    @lombok.Generated
    public ExecFeatureRequest(final String feature, final String command, final java.util.List<String> params) {
        this.feature = feature;
        this.command = command;
        this.params = params;
    }
}
