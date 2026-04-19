package com.linkedin.mcp.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.mcp.server.config.LinkedInDataProperties;
import com.linkedin.mcp.server.domain.LinkedInDataset;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies JSON loading and graceful handling of the sample export bundle.
 */
class LinkedInDataLoaderTest {

    private Path sampleDir;

    @BeforeEach
    void resolveSampleDir() throws Exception {
        sampleDir = Paths.get(LinkedInDataLoaderTest.class.getResource("/sample-linkedin").toURI());
    }

    @Test
    void loadsSampleExport() {
        ObjectMapper mapper = new ObjectMapper();
        LinkedInDataProperties props = new LinkedInDataProperties();
        props.setPath(sampleDir);

        LinkedInDataLoader loader = new LinkedInDataLoader(mapper, props);
        loader.load();

        LinkedInDataset d = loader.getDataset();
        assertThat(d.profile()).isNotNull();
        assertThat(d.profile().resolveFullName()).contains("Sathish");
        assertThat(d.positions()).hasSize(2);
        assertThat(d.skills()).hasSize(3);
        assertThat(d.education()).hasSize(1);
        assertThat(d.certifications()).hasSize(1);
        assertThat(d.projects()).hasSize(1);
        assertThat(d.loadWarnings()).isEmpty();
    }
}
