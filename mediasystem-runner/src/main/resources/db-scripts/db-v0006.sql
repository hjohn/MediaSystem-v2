CREATE TABLE streamstate (
  hash ${Sha256Type} NOT NULL,
  size bigint,
  modtime bigint NOT NULL,

  json ${BinaryType} NOT NULL,
  
  CONSTRAINT streamstate_unique PRIMARY KEY (hash, size, modtime),
  FOREIGN KEY (hash, size, modtime) REFERENCES streamdata(hash, size, modtime) ON DELETE CASCADE
);