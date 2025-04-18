package com.community.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddSectorDto {

    @NotNull
    @JsonProperty("sector_name")
    private String sectorName;

    @JsonProperty("sector_description")
    private String sectorDescription;
}
