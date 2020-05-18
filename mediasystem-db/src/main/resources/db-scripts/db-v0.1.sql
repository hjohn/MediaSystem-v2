CREATE TABLE dbinfo (
  name varchar(50) NOT NULL,
  value varchar(50) NOT NULL,
  
  CONSTRAINT dbinfo_pk PRIMARY KEY (name)
);

INSERT INTO dbinfo (name, value) VALUES ('version', '0');

CREATE TABLE localmedia (
  id varchar(300) NOT NULL,
  scannerid bigint NOT NULL,
  deletetime timestamp,
  json ${BinaryType} NOT NULL,
  
  CONSTRAINT localmedia_id PRIMARY KEY (id)
);