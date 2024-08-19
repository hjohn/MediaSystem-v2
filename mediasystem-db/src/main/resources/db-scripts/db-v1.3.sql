CREATE TABLE identifications (
  location varchar NOT NULL,
  identification ${BinaryType} NOT NULL,
  create_time timestamp with time zone NOT NULL,
  update_time timestamp with time zone NOT NULL,

  CONSTRAINT identifications_pk PRIMARY KEY (location)
);
