package com.rainops.incident.dto;

import com.rainops.incident.entity.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class ChangeIncidentStatusRequest {
    @NotNull(message = "Status is required")
    private IncidentStatus status;

    @Size(max = 1000, message = "Note must not exceed 1000 characters")
    private String note;
}
