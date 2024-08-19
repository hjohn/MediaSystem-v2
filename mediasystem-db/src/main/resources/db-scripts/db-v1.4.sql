CREATE TABLE stream_descriptors (
  content_id integer NOT NULL,
  descriptor ${BinaryType} NOT NULL,
  create_time timestamp with time zone NOT NULL,
  update_time timestamp with time zone NOT NULL,

  CONSTRAINT stream_descriptors_pk PRIMARY KEY (content_id),
  CONSTRAINT stream_descriptors_stream_id_fkey FOREIGN KEY (content_id)
    REFERENCES content_prints (id) ON DELETE CASCADE
);

CREATE TABLE stream_descriptor_snapshots (
  content_id integer NOT NULL,
  index integer NOT NULL,
  image ${BinaryType} NOT NULL,

  CONSTRAINT stream_descriptor_snapshots_pk PRIMARY KEY (content_id, index),
  CONSTRAINT stream_descriptor_snapshots_stream_id_fkey FOREIGN KEY (content_id)
    REFERENCES content_prints (id) ON DELETE CASCADE
);

DROP TABLE stream_metadata;
DROP TABLE stream_metadata_snapshots;