CREATE TABLE stream_identifier (
  stream_id int4 NOT NULL REFERENCES streams(stream_id) ON DELETE CASCADE,
  identifier varchar(1000) NOT NULL,

  CONSTRAINT stream_identifier_pk PRIMARY KEY (stream_id, identifier)
);

ALTER TABLE streams ADD CONSTRAINT streams_stream_ids_fk FOREIGN KEY (stream_id) REFERENCES stream_ids(id) ON DELETE CASCADE;
ALTER TABLE streams ADD COLUMN parent_stream_id int4 REFERENCES streams(stream_id) ON DELETE CASCADE;
ALTER TABLE streams ADD COLUMN match_type varchar(100);
ALTER TABLE streams ADD COLUMN match_ms int8;
ALTER TABLE streams ADD COLUMN match_accuracy float4;
