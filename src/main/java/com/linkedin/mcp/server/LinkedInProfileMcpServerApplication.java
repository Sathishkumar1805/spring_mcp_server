package com.linkedin.mcp.server;

import com.linkedin.mcp.server.config.LinkedInDataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring Boot entry point for the LinkedIn Profile MCP server (STDIO transport).
 */
@SpringBootApplication
@EnableConfigurationProperties(LinkedInDataProperties.class)
public class LinkedInProfileMcpServerApplication {

    /**
     * Starts the MCP server. Claude Desktop communicates over stdin/stdout; keep stdout clean.
     *
     * @param args standard Spring Boot arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(LinkedInProfileMcpServerApplication.class, args);
    }
}
