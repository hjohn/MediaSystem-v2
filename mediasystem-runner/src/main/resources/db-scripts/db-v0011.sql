CREATE TABLE stream_metadata (
  stream_id int4 NOT NULL REFERENCES stream_ids(id) ON DELETE CASCADE,
  modtime bigint NOT NULL,
  version int4 NOT NULL,
  json ${BinaryType} NOT NULL,
  
  CONSTRAINT stream_metadata_pk PRIMARY KEY (stream_id)
);

CREATE TABLE stream_metadata_snapshots (
  stream_id int4 NOT NULL REFERENCES stream_ids(id) ON DELETE CASCADE,
  index int4 NOT NULL,
  image ${BinaryType} NOT NULL,
  
  CONSTRAINT stream_metadata_snapshots_pk PRIMARY KEY (stream_id, index)
);
