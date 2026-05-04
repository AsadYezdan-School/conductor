-- Normalize HTTP headers out of JSONB into a relational table to satisfy 1NF.
-- The headers column was a 1NF violation: the worker iterated individual key-value
-- pairs from it rather than treating it as an opaque blob.

CREATE TABLE job_http_config_headers (
    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    http_config_id  UUID    NOT NULL
                            REFERENCES job_type_http_configs(id) ON DELETE CASCADE,
    header_name     TEXT    NOT NULL,
    header_value    TEXT    NOT NULL
);

-- Migrate any existing JSONB header data into rows
INSERT INTO job_http_config_headers (http_config_id, header_name, header_value)
SELECT id, key, value
FROM   job_type_http_configs, jsonb_each_text(headers)
WHERE  headers IS NOT NULL;

ALTER TABLE job_type_http_configs DROP COLUMN headers;
