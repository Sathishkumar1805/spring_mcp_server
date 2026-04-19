package com.linkedin.mcp.server.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Core profile fields from LinkedIn {@code Profile.json} (structure varies by export locale/version).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ProfileData {

    @JsonProperty("First Name")
    @JsonAlias({"firstName", "localizedFirstName"})
    private String firstName;

    @JsonProperty("Last Name")
    @JsonAlias({"lastName", "localizedLastName"})
    private String lastName;

    @JsonAlias({"headline", "localizedHeadline"})
    private String headline;

    @JsonAlias({"summary"})
    private String summary;

    @JsonProperty("Geo Location")
    @JsonAlias({"location", "locationName", "geoLocationName"})
    private String location;

    @JsonAlias({"industry"})
    private String industry;

    /**
     * @return a display-friendly full name when first/last are present.
     */
    public String resolveFullName() {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName != null ? lastName : "";
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
