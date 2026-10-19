package com.vbforge.atlasdesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Country(
        int id,
        String code,
        String name,
        String continent,
        String capital,
        long countryPopulation,
        long capitalPopulation,
        long area,
        double latitude,
        double longitude
) {}