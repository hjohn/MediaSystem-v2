ALTER TABLE stream_ids RENAME TO content_prints;

ALTER TABLE uris RENAME COLUMN stream_id TO content_id;

ALTER TABLE stream_metadata RENAME COLUMN stream_id TO content_id;

ALTER TABLE stream_metadata_snapshots RENAME COLUMN stream_id TO content_id;

ALTER TABLE streams RENAME COLUMN stream_id TO content_id;
ALTER TABLE streams RENAME COLUMN parent_stream_id TO parent_content_id;

ALTER TABLE stream_identifier RENAME COLUMN stream_id TO content_id;

ALTER TABLE streamstate RENAME COLUMN stream_id TO content_id;