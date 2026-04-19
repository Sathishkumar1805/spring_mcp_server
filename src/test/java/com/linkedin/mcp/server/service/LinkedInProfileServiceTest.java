package com.linkedin.mcp.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.mcp.server.config.LinkedInDataProperties;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises MCP tool methods (search, current role) against the sample LinkedIn export.
 */
class LinkedInProfileServiceTest {

    private LinkedInProfileService service;

    @BeforeEach
    void setup() throws Exception {
        Path sampleDir = Paths.get(LinkedInProfileServiceTest.class.getResource("/sample-linkedin").toURI());
        ObjectMapper mapper = new ObjectMapper();
        LinkedInDataProperties props = new LinkedInDataProperties();
        props.setPath(sampleDir);
        LinkedInDataLoader loader = new LinkedInDataLoader(mapper, props);
        loader.load();
        service = new LinkedInProfileService(loader);
    }

    @Test
    void searchExperience_findsTestOrganizationAndHighlights() {
        String out = service.searchExperience("Test Organization");
        assertThat(out).contains("Test Organization");
        assertThat(out).contains("Senior Java Full Stack Architect");
    }

    @Test
    void getCurrentRole_returnsTestOrganizationArchitect() {
        String out = service.getCurrentRole();
        assertThat(out).contains("Test Organization");
        assertThat(out).contains("Senior Java Full Stack Architect");
    }

    @Test
    void getSkills_filtersByCategory() {
        String all = service.getSkills(null);
        assertThat(all).contains("Spring Boot");

        String techOnly = service.getSkills("Technical");
        assertThat(techOnly).contains("Angular");
    }

    @Test
    void analyzeExpertise_returnsRankedLines() {
        String out = service.analyzeExpertise(3);
        assertThat(out).contains("## Expertise analysis");
    }
}
