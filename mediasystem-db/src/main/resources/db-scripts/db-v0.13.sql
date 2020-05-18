CREATE TABLE streams (
  stream_id int4 NOT NULL,
  scanner_id int4 NOT NULL,
  lastenrichtime int8,
  nextenrichtime int8,
  json ${BinaryType} NOT NULL,
  
  CONSTRAINT streams_pk PRIMARY KEY (stream_id)
);

CREATE TABLE descriptors (
  identifier varchar(1000) NOT NULL,
  lastusedtime int8 NOT NULL,
  json ${BinaryType} NOT NULL,
  
  CONSTRAINT descriptors_pk PRIMARY KEY (identifier)
);

DROP TABLE localmedia;