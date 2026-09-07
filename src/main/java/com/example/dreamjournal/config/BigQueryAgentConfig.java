package com.example.dreamjournal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryAgentConfig {

    private final String projectId;
    private final String location;
    private final String agentId;

    public BigQueryAgentConfig(
            @Value("${google.cloud.project-id}") String projectId,
            @Value("${google.cloud.data-agent.location}") String location,
            @Value("${google.cloud.data-agent.id}") String agentId
    ) {
        this.projectId = projectId;
        this.location = location;
        this.agentId = agentId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getLocation() {
        return location;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentResourceName() {
        return String.format(
                "projects/%s/locations/%s/dataAgents/%s",
                projectId,
                location,
                agentId
        );
    }

    public String getParent() {
        return String.format(
                "projects/%s/locations/%s",
                projectId,
                location
        );
    }
}