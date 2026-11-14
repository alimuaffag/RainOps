package com.rainops.incident.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rainops.incident.entity.IncidentPriority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class UpdateIncidentRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @NotBlank(message = "Location description is required")
    @Size(max = 500, message = "Location description must not exceed 500 characters")
    private String locationDescription;

    @DecimalMin(value = "-90", message = "Latitude must be at least -90")
    @DecimalMax(value = "90", message = "Latitude must not exceed 90")
    @Digits(integer = 3, fraction = 6, message = "Latitude supports up to 6 decimal places")
    private BigDecimal latitude;

    @DecimalMin(value = "-180", message = "Longitude must be at least -180")
    @DecimalMax(value = "180", message = "Longitude must not exceed 180")
    @Digits(integer = 3, fraction = 6, message = "Longitude supports up to 6 decimal places")
    private BigDecimal longitude;

    @NotNull(message = "Priority is required")
    private IncidentPriority priority;

    @JsonIgnore
    @AssertTrue(message = "Latitude and longitude must be supplied together")
    public boolean isCoordinatesComplete() {
        return (latitude == null) == (longitude == null);
    }
}

