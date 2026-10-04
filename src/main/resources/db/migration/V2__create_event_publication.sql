-- The event publication registry of Spring Modulith.
--
-- With the JPA variant the table is mapped by a framework entity, so the schema has to
-- exist and match it. Creating it here instead of letting ddl-auto do it keeps every
-- table in the project under the same migration history.
--
-- `serialized_event` is TEXT rather than the VARCHAR(255) Hibernate would generate by
-- default: the column holds the JSON payload of the event, and 255 characters is a limit
-- that only shows up the day an event grows a field.
CREATE TABLE event_publication (
    id               UUID         PRIMARY KEY,
    listener_id      VARCHAR(512) NOT NULL,
    event_type       VARCHAR(512) NOT NULL,
    serialized_event TEXT         NOT NULL,
    publication_date TIMESTAMPTZ  NOT NULL,
    completion_date  TIMESTAMPTZ
);

-- The query that matters is "what is still pending", and it runs on every restart.
CREATE INDEX idx_event_publication_incomplete
    ON event_publication (completion_date, publication_date)
    WHERE completion_date IS NULL;
