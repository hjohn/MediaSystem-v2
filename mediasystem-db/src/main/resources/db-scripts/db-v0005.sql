CREATE TABLE images (
  url varchar(1000) NOT NULL,
  creationtime timestamp NOT NULL,
  accesstime timestamp NOT NULL,
  image ${BinaryType} NOT NULL,
  
  CONSTRAINT images_url PRIMARY KEY (url)
);