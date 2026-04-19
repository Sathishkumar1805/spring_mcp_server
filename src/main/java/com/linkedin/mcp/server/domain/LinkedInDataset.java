package com.linkedin.mcp.server.domain;

import java.util.List;

/**
 * In-memory snapshot of all LinkedIn JSON files loaded from disk.
 */
public record LinkedInDataset(
        ProfileData profile,
        List<Position> positions,
        List<Skill> skills,
        List<Education> education,
        List<Certification> certifications,
        List<Project> projects,
        List<String> loadWarnings) {

    /**
     * @return empty dataset used before load or when the export directory is unusable
     */
    public static LinkedInDataset empty() {
        return new LinkedInDataset(null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
