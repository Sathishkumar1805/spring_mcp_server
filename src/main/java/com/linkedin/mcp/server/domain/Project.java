package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Project entry from LinkedIn {@code Projects.json}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Project {

    @JsonAlias({"title", "name"})
    private String title;

    private String description;

    @JsonAlias({"startDate", "startedOn"})
    private Object startDate;

    @JsonAlias({"endDate", "endedOn"})
    private Object endDate;

    @JsonAlias({"url"})
    private String url;
}
