CREATE TABLE streamdata (
  id ${SerialType},
  url varchar(2000) NOT NULL,
  hash ${Sha256Type} NOT NULL,
  size bigint NOT NULL,
  modtime bigint NOT NULL,
  json ${BinaryType} NOT NULL,
  
  CONSTRAINT streamdata_id PRIMARY KEY (id),
  CONSTRAINT streamdata_url UNIQUE (url, size, modtime),
  CONSTRAINT streamdata_hash UNIQUE (hash, size, modtime)
);