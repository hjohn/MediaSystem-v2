CREATE INDEX content_prints_lastseentime_idx ON content_prints (lastseentime);

DROP TABLE stream_identifier;
DROP TABLE streams;

CREATE TABLE streams (
  id serial4 NOT NULL PRIMARY KEY,
  parent_id int4 REFERENCES streams(id) ON DELETE CASCADE,

  content_id int4 NOT NULL REFERENCES content_prints(id) ON DELETE CASCADE,
  scanner_id int4 NOT NULL,
  name varchar(1000) NOT NULL,
  lastenrichtime int8,
  nextenrichtime int8,
  json ${BinaryType} NOT NULL,
  creation_ms int8 NOT NULL,

  match_type varchar(100),
  match_ms int8,
  match_accuracy float4,

  CONSTRAINT streams_unique UNIQUE (content_id, scanner_id, name)
);

CREATE TABLE stream_identifier (
  stream_id int4 NOT NULL REFERENCES streams(id) ON DELETE CASCADE,
  identifier varchar(1000) NOT NULL,

  CONSTRAINT stream_identifier_pk PRIMARY KEY (stream_id, identifier)
);
