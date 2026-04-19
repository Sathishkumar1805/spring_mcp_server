package com.linkedin.mcp.server.config;

import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the LinkedIn JSON export directory.
 */
@ConfigurationProperties(prefix = "linkedin.data")
@Getter
@Setter
public class LinkedInDataProperties {

    /**
     * Directory containing LinkedIn export JSON files (Profile.json, Positions.json, ...).
     */
    private Path path;
}
