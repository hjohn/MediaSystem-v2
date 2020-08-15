CREATE TABLE dbinfo (
  name varchar(50) NOT NULL,
  value varchar(50) NOT NULL,

  CONSTRAINT dbinfo_pk PRIMARY KEY (name)
);

INSERT INTO dbinfo (name, value) VALUES ('version', '0.0');

CREATE TABLE content_prints (
  id ${SerialType},
  hash ${Sha256Type} NOT NULL,
  size bigint,
  modtime bigint NOT NULL,
  lastseentime bigint,

  CONSTRAINT streamdata_id PRIMARY KEY (id),
  CONSTRAINT streamdata_hash UNIQUE (hash, size, modtime)
);

CREATE INDEX content_prints_lastseentime_idx ON content_prints (lastseentime);

CREATE TABLE descriptors (
  identifier varchar(1000),
  lastusedtime bigint NOT NULL,
  json ${BinaryType} NOT NULL,

  CONSTRAINT descriptors_pk PRIMARY KEY (identifier)
);

CREATE TABLE images (
  url varchar(1000),
  creationtime timestamp NOT NULL,
  accesstime timestamp NOT NULL,
  image ${BinaryType} NOT NULL,
  logical_key varchar(1000),

  CONSTRAINT images_url PRIMARY KEY (url)
);

CREATE TABLE settings (
  id ${SerialType},
  system varchar(100) NOT NULL,
  persistlevel varchar(20) NOT NULL,
  name varchar(2000) NOT NULL,
  value varchar(2000) NOT NULL,
  lastupdated timestamp NOT NULL,

  CONSTRAINT settings_id PRIMARY KEY (id),
  CONSTRAINT settings_system_key UNIQUE (system, name)
);

CREATE TABLE streams (
  id ${SerialType},
  parent_id integer,
  content_id integer NOT NULL,
  scanner_id integer NOT NULL,
  name varchar(1000) NOT NULL,
  lastenrichtime bigint,
  nextenrichtime bigint,
  json ${BinaryType} NOT NULL,
  creation_ms bigint NOT NULL,
  match_type varchar(100),
  match_ms bigint,
  match_accuracy real,

  CONSTRAINT streams_pkey PRIMARY KEY (id),
  CONSTRAINT streams_content_id_fkey FOREIGN KEY (content_id)
      REFERENCES content_prints (id) ON DELETE CASCADE,
  CONSTRAINT streams_parent_id_fkey FOREIGN KEY (parent_id)
      REFERENCES streams (id) ON DELETE CASCADE,
  CONSTRAINT streams_unique UNIQUE (content_id, scanner_id, name)
);

CREATE TABLE stream_identifier (
  stream_id integer NOT NULL,
  identifier varchar(1000) NOT NULL,

  CONSTRAINT stream_identifier_pk PRIMARY KEY (stream_id, identifier),
  CONSTRAINT stream_identifier_stream_id_fkey FOREIGN KEY (stream_id)
      REFERENCES streams (id) ON DELETE CASCADE
);

CREATE TABLE stream_metadata (
  content_id integer NOT NULL,
  modtime bigint NOT NULL,
  version integer NOT NULL,
  json ${BinaryType} NOT NULL,

  CONSTRAINT stream_metadata_pk PRIMARY KEY (content_id),
  CONSTRAINT stream_metadata_stream_id_fkey FOREIGN KEY (content_id)
      REFERENCES content_prints (id) ON DELETE CASCADE
);

CREATE TABLE stream_metadata_snapshots (
  content_id integer NOT NULL,
  index integer NOT NULL,
  image ${BinaryType} NOT NULL,

  CONSTRAINT stream_metadata_snapshots_pk PRIMARY KEY (content_id, index),
  CONSTRAINT stream_metadata_snapshots_stream_id_fkey FOREIGN KEY (content_id)
      REFERENCES content_prints (id) ON DELETE CASCADE
);

CREATE TABLE streamstate (
  json ${BinaryType} NOT NULL,
  content_id integer NOT NULL,

  CONSTRAINT streamstate_id_pk PRIMARY KEY (content_id),
  CONSTRAINT streamstate_stream_id_fkey FOREIGN KEY (content_id)
      REFERENCES content_prints (id) ON DELETE CASCADE
);

CREATE TABLE uris (
  id ${SerialType},
  content_id integer NOT NULL,
  uri varchar(2000) NOT NULL,

  CONSTRAINT uris_id_pk PRIMARY KEY (id),
  CONSTRAINT uris_stream_id_fkey FOREIGN KEY (content_id)
      REFERENCES content_prints (id) ON DELETE CASCADE,
  CONSTRAINT uris_uri_unique UNIQUE (uri)
);
