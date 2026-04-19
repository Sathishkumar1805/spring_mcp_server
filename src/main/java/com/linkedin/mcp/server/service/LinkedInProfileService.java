package com.linkedin.mcp.server.service;

import com.linkedin.mcp.server.domain.Certification;
import com.linkedin.mcp.server.domain.Education;
import com.linkedin.mcp.server.domain.LinkedInDataset;
import com.linkedin.mcp.server.domain.Position;
import com.linkedin.mcp.server.domain.ProfileData;
import com.linkedin.mcp.server.domain.Skill;
import com.linkedin.mcp.server.support.ExpertiseAnalyzer;
import com.linkedin.mcp.server.support.LinkedInText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP-exposed tools that surface LinkedIn export data as markdown-oriented text for LLM consumption.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LinkedInProfileService {

    private final LinkedInDataLoader linkedInDataLoader;

    /**
     * Basic profile information including name, headline, and summary.
     *
     * @return markdown summary
     */
    @Tool(
            name = "get_profile_summary",
            description =
                    "Get basic LinkedIn profile information including name, headline, and summary. "
                            + "Use when you need identity, positioning statement, or top-level career narrative.")
    public String getProfileSummary() {
        log.debug("MCP tool invoked: get_profile_summary");
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Profile summary\n\n");
        appendWarnings(sb, d);
        ProfileData p = d.profile();
        if (p == null) {
            sb.append("_No Profile.json loaded._\n");
            return sb.toString();
        }
        String name = p.resolveFullName();
        if (!name.isBlank()) {
            sb.append("**Name:** ").append(name).append("\n\n");
        }
        if (notBlank(p.getHeadline())) {
            sb.append("**Headline:** ").append(p.getHeadline().trim()).append("\n\n");
        }
        if (notBlank(p.getSummary())) {
            sb.append("**Summary:**\n\n").append(p.getSummary().trim()).append("\n\n");
        }
        if (notBlank(p.getLocation())) {
            sb.append("**Location:** ").append(p.getLocation().trim()).append("\n\n");
        }
        if (notBlank(p.getIndustry())) {
            sb.append("**Industry:** ").append(p.getIndustry().trim()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Complete work history in chronological order (most recent / current roles first when parseable).
     *
     * @return markdown list of positions
     */
    @Tool(
            name = "get_work_experience",
            description =
                    "Get complete work history from LinkedIn profile in chronological order (current role first when marked Present). "
                            + "Use for resume bullets, interview prep, or timeline questions.")
    public String getWorkExperience() {
        log.debug("MCP tool invoked: get_work_experience");
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Work experience\n\n");
        appendWarnings(sb, d);
        List<Position> ordered = ExpertiseAnalyzer.chronologicalForDisplay(d.positions());
        if (ordered.isEmpty()) {
            sb.append("_No Positions.json data loaded._\n");
            return sb.toString();
        }
        int i = 1;
        for (Position pos : ordered) {
            sb.append("### ")
                    .append(i++)
                    .append(". ")
                    .append(safe(pos.getTitle()))
                    .append(" — **")
                    .append(safe(pos.getCompanyName()))
                    .append("**\n\n");
            sb.append("- **Duration:** ")
                    .append(LinkedInText.formatDate(pos.getStartDate()))
                    .append(" → ")
                    .append(LinkedInText.formatDate(pos.getEndDate()))
                    .append("\n");
            if (notBlank(pos.getLocation())) {
                sb.append("- **Location:** ").append(pos.getLocation().trim()).append("\n");
            }
            if (notBlank(pos.getEmploymentType())) {
                sb.append("- **Employment type:** ").append(pos.getEmploymentType().trim()).append("\n");
            }
            sb.append("\n");
            if (notBlank(pos.getDescription())) {
                sb.append(pos.getDescription().trim()).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Keyword search across titles, companies, locations, and descriptions.
     *
     * @param keyword technology, company, role fragment, etc.
     * @return matching roles with highlighted snippets
     */
    @Tool(
            name = "search_experience",
            description =
                    "Search work experience by keyword (technology, company, role, project name, etc.). "
                            + "Returns matching positions with short highlighted excerpts from descriptions when possible.")
    public String searchExperience(
            @ToolParam(description = "Search term to find in titles, companies, locations, and descriptions", required = true)
                    String keyword) {
        log.debug("MCP tool invoked: search_experience keyword={}", keyword);
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Experience search\n\n");
        appendWarnings(sb, d);
        if (keyword == null || keyword.isBlank()) {
            sb.append("_Provide a non-empty keyword._\n");
            return sb.toString();
        }
        String k = keyword.trim();
        List<Position> hits = d.positions().stream()
                .filter(p -> containsInsensitive(p, k))
                .collect(Collectors.toCollection(ArrayList::new));
        if (hits.isEmpty()) {
            sb.append("No positions matched **").append(k).append("**.\n");
            return sb.toString();
        }
        sb.append("Matches for **").append(k).append("** (").append(hits.size()).append("):\n\n");
        int n = 1;
        for (Position p : hits) {
            sb.append("### ")
                    .append(n++)
                    .append(". ")
                    .append(LinkedInText.highlightKeyword(safe(p.getTitle()), k))
                    .append(" — ")
                    .append(LinkedInText.highlightKeyword(safe(p.getCompanyName()), k))
                    .append("\n\n");
            sb.append("- **Duration:** ")
                    .append(LinkedInText.formatDate(p.getStartDate()))
                    .append(" → ")
                    .append(LinkedInText.formatDate(p.getEndDate()))
                    .append("\n");
            if (notBlank(p.getDescription())) {
                String snip = LinkedInText.snippetAroundKeyword(p.getDescription(), k, 120);
                sb.append("- **Context:** ")
                        .append(LinkedInText.highlightKeyword(snip, k))
                        .append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Skills with optional category filter.
     *
     * @param category optional category substring
     * @return markdown list
     */
    @Tool(
            name = "get_skills",
            description =
                    "Get all skills from LinkedIn profile, optionally filtered by category name (substring match, case-insensitive). "
                            + "Includes endorsement counts when present in the export.")
    public String getSkills(
            @ToolParam(
                            description = "Optional skill category filter (substring); omit to return all skills",
                            required = false)
                    String category) {
        log.debug("MCP tool invoked: get_skills category={}", category);
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Skills\n\n");
        appendWarnings(sb, d);
        List<Skill> skills = d.skills();
        if (skills.isEmpty()) {
            sb.append("_No Skills.json data loaded._\n");
            return sb.toString();
        }
        List<Skill> filtered = skills.stream()
                .filter(s -> category == null
                        || category.isBlank()
                        || (s.getCategory() != null
                                && s.getCategory().toLowerCase(Locale.ROOT).contains(category.toLowerCase(Locale.ROOT))))
                .toList();
        if (filtered.isEmpty()) {
            sb.append("_No skills matched category filter._\n");
            return sb.toString();
        }
        for (Skill s : filtered) {
            sb.append("- **").append(safe(s.getName())).append("**");
            if (notBlank(s.getCategory())) {
                sb.append(" _(category: ").append(s.getCategory()).append(")_");
            }
            if (s.getEndorsementCount() != null) {
                sb.append(" — endorsements: ").append(s.getEndorsementCount());
            }
            sb.append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Professional certifications.
     *
     * @return markdown list
     */
    @Tool(
            name = "get_certifications",
            description =
                    "Get professional certifications and credentials (name, issuer, dates) from the LinkedIn export.")
    public String getCertifications() {
        log.debug("MCP tool invoked: get_certifications");
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Certifications\n\n");
        appendWarnings(sb, d);
        if (d.certifications().isEmpty()) {
            sb.append("_No Certifications.json data loaded._\n");
            return sb.toString();
        }
        for (Certification c : d.certifications()) {
            sb.append("- **").append(safe(c.getName())).append("**");
            if (notBlank(c.getAuthority())) {
                sb.append(" — ").append(c.getAuthority().trim());
            }
            sb.append("\n");
            sb.append("  - **Issued / valid:** ")
                    .append(LinkedInText.formatDate(c.getStartDate()))
                    .append(" → ")
                    .append(LinkedInText.formatDate(c.getEndDate()))
                    .append("\n");
            if (notBlank(c.getLicenseNumber())) {
                sb.append("  - **Credential ID:** ").append(c.getLicenseNumber().trim()).append("\n");
            }
            if (notBlank(c.getUrl())) {
                sb.append("  - **URL:** ").append(c.getUrl().trim()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Education history.
     *
     * @return markdown list
     */
    @Tool(
            name = "get_education",
            description = "Get educational background: school, degree, field of study, and dates from the LinkedIn export.")
    public String getEducation() {
        log.debug("MCP tool invoked: get_education");
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Education\n\n");
        appendWarnings(sb, d);
        if (d.education().isEmpty()) {
            sb.append("_No Education.json data loaded._\n");
            return sb.toString();
        }
        for (Education e : d.education()) {
            sb.append("- **").append(safe(e.getSchoolName())).append("**");
            if (notBlank(e.getDegreeName())) {
                sb.append(" — ").append(e.getDegreeName().trim());
            }
            if (notBlank(e.getFieldOfStudy())) {
                sb.append(", ").append(e.getFieldOfStudy().trim());
            }
            sb.append("\n");
            sb.append("  - **Dates:** ")
                    .append(LinkedInText.formatDate(e.getStartDate()))
                    .append(" → ")
                    .append(LinkedInText.formatDate(e.getEndDate()))
                    .append("\n");
            if (notBlank(e.getDescription())) {
                sb.append("  - ").append(e.getDescription().trim()).append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Current role (LinkedIn end date Present / empty).
     *
     * @return markdown section
     */
    @Tool(
            name = "get_current_role",
            description =
                    "Get details about the current employment position (end date marked Present or empty). "
                            + "If multiple current roles exist, picks the strongest match by latest start date when parseable.")
    public String getCurrentRole() {
        log.debug("MCP tool invoked: get_current_role");
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Current role\n\n");
        appendWarnings(sb, d);
        Optional<Position> current = d.positions().stream()
                .filter(p -> LinkedInText.isPresentEndDate(p.getEndDate()))
                .max(Comparator.comparing(
                        (Position p) -> LinkedInText.parseYearMonthIndex(p.getStartDate()),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        if (current.isEmpty()) {
            sb.append("_No current role detected (look for endDate = Present in Positions.json)._ \n");
            return sb.toString();
        }
        Position p = current.get();
        sb.append("### ").append(safe(p.getTitle())).append(" @ **").append(safe(p.getCompanyName())).append("**\n\n");
        sb.append("- **Duration:** ")
                .append(LinkedInText.formatDate(p.getStartDate()))
                .append(" → ")
                .append(LinkedInText.formatDate(p.getEndDate()))
                .append("\n");
        if (notBlank(p.getLocation())) {
            sb.append("- **Location:** ").append(p.getLocation().trim()).append("\n");
        }
        if (notBlank(p.getEmploymentType())) {
            sb.append("- **Employment type:** ").append(p.getEmploymentType().trim()).append("\n");
        }
        sb.append("\n");
        if (notBlank(p.getDescription())) {
            sb.append(p.getDescription().trim()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Expertise themes ranked from duration-weighted signals in titles and descriptions.
     *
     * @param topN number of themes
     * @return ranked markdown list
     */
    @Tool(
            name = "analyze_expertise",
            description =
                    "Analyze profile to identify top areas of expertise based on experience duration and recency. "
                            + "Uses weighted term analysis from position titles and descriptions (heuristic, not industry taxonomy).")
    public String analyzeExpertise(
            @ToolParam(description = "Number of top expertise areas to return (default 5)", required = false) Integer topN) {
        int n = topN == null || topN < 1 ? 5 : Math.min(topN, 25);
        log.debug("MCP tool invoked: analyze_expertise topN={}", n);
        LinkedInDataset d = linkedInDataLoader.getDataset();
        StringBuilder sb = new StringBuilder();
        sb.append("## Expertise analysis\n\n");
        appendWarnings(sb, d);
        if (d.positions().isEmpty()) {
            sb.append("_No Positions.json data loaded._\n");
            return sb.toString();
        }
        sb.append("_Heuristic ranking from work history (").append(n).append(" themes):_\n\n");
        List<String> lines = ExpertiseAnalyzer.rankExpertise(d.positions(), n);
        for (String line : lines) {
            sb.append(line).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    private static void appendWarnings(StringBuilder sb, LinkedInDataset d) {
        if (!d.loadWarnings().isEmpty()) {
            sb.append("> **Load notes:** ");
            sb.append(String.join(" • ", d.loadWarnings()));
            sb.append("\n\n");
        }
    }

    private static boolean containsInsensitive(Position p, String keyword) {
        String needle = keyword.toLowerCase(Locale.ROOT);
        return lc(p.getTitle()).contains(needle)
                || lc(p.getCompanyName()).contains(needle)
                || lc(p.getLocation()).contains(needle)
                || lc(p.getDescription()).contains(needle);
    }

    private static String lc(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String safe(String s) {
        return Objects.toString(s, "").trim();
    }
}
