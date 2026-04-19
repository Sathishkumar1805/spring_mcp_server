package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Work experience entry, aligned with LinkedIn {@code Positions.json} exports.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Position {

    @JsonAlias({"company", "Company Name"})
    private String companyName;

    @JsonAlias({"title", "jobTitle"})
    private String title;

    private String description;

    private String location;

    @JsonAlias({"startDate", "startedOn"})
    private Object startDate;

    @JsonAlias({"endDate", "endedOn"})
    private Object endDate;

    @JsonAlias({"employmentType"})
    private String employmentType;
}
