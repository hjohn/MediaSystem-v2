
# Add new column:
ALTER TABLE content_prints ADD COLUMN creation_ms bigint;

# By default, set to time of content:
UPDATE content_prints SET creation_ms = modtime;

# Update as many as possible from streams:
UPDATE content_prints cp SET creation_ms = s.creation_ms FROM (SELECT * FROM streams) AS s WHERE cp.id = s.content_id;

# Put on the NOT NULL constraint:
ALTER TABLE content_prints ALTER COLUMN creation_ms SET NOT NULL;