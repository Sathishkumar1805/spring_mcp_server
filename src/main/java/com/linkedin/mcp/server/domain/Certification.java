package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/**
 * Certification / license entry from LinkedIn {@code Certifications.json} (or similar export files).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Certification {

    @JsonAlias({"title"})
    private String name;

    @JsonAlias({"issuer", "companyName"})
    private String authority;

    @JsonAlias({"licenseNumber"})
    private String licenseNumber;

    @JsonAlias({"url"})
    private String url;

    @JsonAlias({"startDate", "issuedOn", "startedOn"})
    private Object startDate;

    @JsonAlias({"endDate", "expiresOn", "endedOn"})
    private Object endDate;
}
