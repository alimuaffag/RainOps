CREATE TABLE incident_status_history (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    note VARCHAR(1000),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_incident_status_history_incident
        FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT chk_history_from_status
        CHECK (from_status IN ('NEW', 'IN_PROGRESS', 'RESOLVED')),
    CONSTRAINT chk_history_to_status
        CHECK (to_status IN ('NEW', 'IN_PROGRESS', 'RESOLVED'))
);

CREATE INDEX idx_incident_status_history_incident_changed
    ON incident_status_history (incident_id, changed_at, id);
