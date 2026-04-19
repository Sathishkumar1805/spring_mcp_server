package com.linkedin.mcp.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.mcp.server.config.LinkedInDataProperties;
import com.linkedin.mcp.server.domain.Certification;
import com.linkedin.mcp.server.domain.Education;
import com.linkedin.mcp.server.domain.LinkedInDataset;
import com.linkedin.mcp.server.domain.Position;
import com.linkedin.mcp.server.domain.ProfileData;
import com.linkedin.mcp.server.domain.Project;
import com.linkedin.mcp.server.domain.Skill;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Loads LinkedIn export JSON files once at startup and caches them in memory.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LinkedInDataLoader {

    private static final String PROFILE = "Profile.json";
    private static final String POSITIONS = "Positions.json";
    private static final String SKILLS = "Skills.json";
    private static final String EDUCATION = "Education.json";
    private static final String CERTIFICATIONS = "Certifications.json";
    private static final String CERTIFICATIONS_ALT = "Licenses & Certifications.json";
    private static final String PROJECTS = "Projects.json";

    private final ObjectMapper objectMapper;
    private final LinkedInDataProperties linkedInDataProperties;

    private final AtomicReference<LinkedInDataset> cached = new AtomicReference<>(LinkedInDataset.empty());

    /**
     * Loads JSON from {@code linkedin.data.path} (best-effort per file).
     */
    @PostConstruct
    public void load() {
        List<String> warnings = new ArrayList<>();
        Path root = linkedInDataProperties.getPath();
        if (root == null) {
            warnings.add("linkedin.data.path is not set; set LINKEDIN_DATA_PATH or linkedin.data.path.");
            log.warn("LinkedIn data path is null; serving empty dataset");
            cached.set(new LinkedInDataset(null, List.of(), List.of(), List.of(), List.of(), List.of(), warnings));
            return;
        }
        if (!Files.exists(root)) {
            warnings.add("Data directory does not exist: " + root.toAbsolutePath());
            log.warn("LinkedIn data directory missing: {}", root.toAbsolutePath());
            cached.set(new LinkedInDataset(null, List.of(), List.of(), List.of(), List.of(), List.of(), warnings));
            return;
        }
        if (!Files.isDirectory(root)) {
            warnings.add("Data path is not a directory: " + root.toAbsolutePath());
            log.warn("linkedin.data.path is not a directory: {}", root.toAbsolutePath());
            cached.set(new LinkedInDataset(null, List.of(), List.of(), List.of(), List.of(), List.of(), warnings));
            return;
        }

        ProfileData profile = readProfile(root, warnings);
        List<Position> positions = readList(root, POSITIONS, new TypeReference<>() {}, warnings);
        List<Skill> skills = readList(root, SKILLS, new TypeReference<>() {}, warnings);
        List<Education> education = readList(root, EDUCATION, new TypeReference<>() {}, warnings);
        List<Certification> certifications = readCertifications(root, warnings);
        List<Project> projects = readList(root, PROJECTS, new TypeReference<>() {}, warnings);

        cached.set(new LinkedInDataset(profile, positions, skills, education, certifications, projects, warnings));
        log.info(
                "LinkedIn data loaded from {} (profile={}, positions={}, skills={}, education={}, certs={}, projects={}, warnings={})",
                root.toAbsolutePath(),
                profile != null,
                positions.size(),
                skills.size(),
                education.size(),
                certifications.size(),
                projects.size(),
                warnings.size());
    }

    /**
     * @return latest loaded dataset snapshot
     */
    public LinkedInDataset getDataset() {
        return cached.get();
    }

    private ProfileData readProfile(Path root, List<String> warnings) {
        Path file = root.resolve(PROFILE);
        if (!Files.exists(file)) {
            warnings.add("Missing file: " + PROFILE);
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(file.toFile());
            if (node == null || node.isNull()) {
                return null;
            }
            if (node.isArray() && node.size() > 0) {
                return objectMapper.treeToValue(node.get(0), ProfileData.class);
            }
            if (node.isObject()) {
                return objectMapper.treeToValue(node, ProfileData.class);
            }
            warnings.add(PROFILE + " has unexpected JSON shape");
            return null;
        } catch (IOException e) {
            warnings.add("Failed to parse " + PROFILE + ": " + e.getMessage());
            log.debug("Profile parse failure", e);
            return null;
        }
    }

    private List<Certification> readCertifications(Path root, List<String> warnings) {
        Path primary = root.resolve(CERTIFICATIONS);
        if (Files.exists(primary)) {
            return readList(root, CERTIFICATIONS, new TypeReference<>() {}, warnings);
        }
        Path alt = root.resolve(CERTIFICATIONS_ALT);
        if (Files.exists(alt)) {
            return readList(root, CERTIFICATIONS_ALT, new TypeReference<>() {}, warnings);
        }
        warnings.add("Missing file: " + CERTIFICATIONS + " (optional alternate: " + CERTIFICATIONS_ALT + ")");
        return List.of();
    }

    private <T> List<T> readList(Path root, String fileName, TypeReference<List<T>> type, List<String> warnings) {
        Path file = root.resolve(fileName);
        if (!Files.exists(file)) {
            warnings.add("Missing file: " + fileName);
            return List.of();
        }
        try {
            List<T> list = objectMapper.readValue(file.toFile(), type);
            return list != null ? list : List.of();
        } catch (IOException e) {
            warnings.add("Failed to parse " + fileName + ": " + e.getMessage());
            log.debug("Parse failure for {}", fileName, e);
            return List.of();
        }
    }
}
