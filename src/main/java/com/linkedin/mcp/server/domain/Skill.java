package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Skill entry from LinkedIn {@code Skills.json}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Skill {

    @JsonAlias({"skillName"})
    private String name;

    @JsonAlias({"categoryName", "skillCategory"})
    private String category;

    @JsonAlias({"endorsementCount", "endorsementsCount", "endorsements"})
    private Integer endorsementCount;
}
