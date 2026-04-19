package com.linkedin.mcp.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.mcp.server.config.LinkedInDataProperties;
import com.linkedin.mcp.server.domain.LinkedInDataset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ensures missing files produce warnings without crashing the server.
 */
class LinkedInDataLoaderMissingFilesTest {

    @Test
    void missingDirectory_yieldsWarnings(@TempDir Path emptyDir) {
        ObjectMapper mapper = new ObjectMapper();
        LinkedInDataProperties props = new LinkedInDataProperties();
        props.setPath(emptyDir.resolve("nonexistent"));

        LinkedInDataLoader loader = new LinkedInDataLoader(mapper, props);
        loader.load();

        LinkedInDataset d = loader.getDataset();
        assertThat(d.loadWarnings()).isNotEmpty();
    }

    @Test
    void partialFiles_loadsWhatExists(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("Profile.json"), "[{\"firstName\":\"A\",\"lastName\":\"B\",\"headline\":\"H\"}]");

        ObjectMapper mapper = new ObjectMapper();
        LinkedInDataProperties props = new LinkedInDataProperties();
        props.setPath(dir);

        LinkedInDataLoader loader = new LinkedInDataLoader(mapper, props);
        loader.load();

        LinkedInDataset d = loader.getDataset();
        assertThat(d.profile().getFirstName()).isEqualTo("A");
        assertThat(d.loadWarnings()).isNotEmpty();
    }

    @Test
    void malformedJson_recordsWarning(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("Profile.json"), "[{\"firstName\":\"OK\"}]");
        Files.writeString(dir.resolve("Positions.json"), "{ not json");

        ObjectMapper mapper = new ObjectMapper();
        LinkedInDataProperties props = new LinkedInDataProperties();
        props.setPath(dir);

        LinkedInDataLoader loader = new LinkedInDataLoader(mapper, props);
        loader.load();

        LinkedInDataset d = loader.getDataset();
        assertThat(d.profile()).isNotNull();
        assertThat(d.positions()).isEmpty();
        assertThat(d.loadWarnings().stream().anyMatch(w -> w.contains("Positions.json"))).isTrue();
    }
}
