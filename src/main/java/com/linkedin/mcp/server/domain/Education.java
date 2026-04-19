package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Education entry from LinkedIn {@code Education.json}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Education {

    @JsonAlias({"schoolName", "school", "School Name"})
    private String schoolName;

    @JsonAlias({"degreeName", "degree"})
    private String degreeName;

    @JsonAlias({"fieldOfStudy", "field_of_study"})
    private String fieldOfStudy;

    @JsonAlias({"startDate", "startedOn", "start"})
    private Object startDate;

    @JsonAlias({"endDate", "endedOn", "end"})
    private Object endDate;

    private String description;
}
