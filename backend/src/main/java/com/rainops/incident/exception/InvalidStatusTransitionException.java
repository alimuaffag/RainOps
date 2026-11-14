package com.rainops.incident.exception;

import com.rainops.incident.entity.IncidentStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(IncidentStatus from, IncidentStatus to) {
        super("Cannot change incident status from " + from + " to " + to);
    }
}
