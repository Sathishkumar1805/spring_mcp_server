package com.linkedin.mcp.server.config;

import com.linkedin.mcp.server.service.LinkedInProfileService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link org.springframework.ai.tool.annotation.Tool} methods on {@link LinkedInProfileService}
 * as MCP tool callbacks for the Spring AI MCP server.
 */
@Configuration
public class McpToolConfiguration {

    /**
     * Exposes all {@code @Tool} methods on the profile service to the MCP runtime.
     *
     * @param linkedInProfileService tool implementation bean
     * @return provider consumed by MCP server auto-configuration
     */
    @Bean
    public ToolCallbackProvider linkedInProfileToolCallbackProvider(LinkedInProfileService linkedInProfileService) {
        return MethodToolCallbackProvider.builder().toolObjects(linkedInProfileService).build();
    }
}
