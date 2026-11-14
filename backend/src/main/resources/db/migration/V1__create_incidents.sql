-- Incident IDs will be assigned by the backend.
CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    district VARCHAR(100) NOT NULL,
    location_description VARCHAR(500) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,

    CONSTRAINT chk_incidents_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT chk_incidents_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED')),
    CONSTRAINT chk_incidents_latitude
        CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_incidents_longitude
        CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_incidents_coordinates_pair
        CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_priority ON incidents (priority);
CREATE INDEX idx_incidents_created_at ON incidents (created_at DESC);
