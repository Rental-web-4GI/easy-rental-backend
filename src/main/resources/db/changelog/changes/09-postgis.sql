--liquibase formatted sql

--changeset easyrental:09-enable-postgis
CREATE EXTENSION IF NOT EXISTS postgis;

--changeset easyrental:10-agency-location
ALTER TABLE agencies ADD COLUMN IF NOT EXISTS location GEOGRAPHY(POINT, 4326);

UPDATE agencies SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND location IS NULL;

CREATE INDEX IF NOT EXISTS idx_agencies_location ON agencies USING GIST (location);
