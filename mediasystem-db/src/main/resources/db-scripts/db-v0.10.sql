
# Script to change the use of UUID identifiers to a simple id, and to
# change the uri-hash relationship to a one to many:

###########################################################################
# Create uris table:
###########################################################################

CREATE TABLE uris (
  id ${SerialType},
  stream_id int4 NOT NULL REFERENCES streamdata(id) ON DELETE CASCADE,
  uri varchar(2000) NOT NULL,
  
  CONSTRAINT uris_id_pk PRIMARY KEY (id),
  CONSTRAINT uris_uri_unique UNIQUE (uri)
);

INSERT INTO uris (stream_id, uri) SELECT id, url FROM streamdata;

###########################################################################
# Update streamstate table to use id instead of UUID:
###########################################################################

ALTER TABLE streamstate ADD COLUMN stream_id int4;

UPDATE streamstate SET stream_id = sd.id FROM (SELECT id, persistent_identifier FROM streamdata) AS sd WHERE streamdata_id = sd.persistent_identifier;

ALTER TABLE streamstate ALTER COLUMN stream_id SET NOT NULL;
ALTER TABLE streamstate DROP CONSTRAINT streamstate_unique;
ALTER TABLE streamstate DROP CONSTRAINT streamdata_id_fkey;
ALTER TABLE streamstate DROP COLUMN streamdata_id;
ALTER TABLE streamstate ADD CONSTRAINT streamstate_id_pk PRIMARY KEY (stream_id);
ALTER TABLE streamstate ADD CONSTRAINT streamstate_stream_id_fkey FOREIGN KEY (stream_id) REFERENCES streamdata(id) ON DELETE CASCADE;

###########################################################################
# Update localmedia table to use id instead of UUID:
###########################################################################

DELETE FROM localmedia;
ALTER TABLE localmedia ADD COLUMN stream_id int4 NOT NULL REFERENCES streamdata(id) ON DELETE CASCADE;
ALTER TABLE localmedia DROP CONSTRAINT localmedia_id;
ALTER TABLE localmedia DROP COLUMN id;
ALTER TABLE localmedia ADD CONSTRAINT localmedia_unique UNIQUE (stream_id, scannerid);

###########################################################################
# Clean up and rename streamdata table:
###########################################################################

ALTER TABLE streamdata DROP CONSTRAINT streamdata_url;
ALTER TABLE streamdata DROP CONSTRAINT streamdata_identifier;
ALTER TABLE streamdata DROP COLUMN url;
ALTER TABLE streamdata DROP COLUMN persistent_identifier;
ALTER TABLE streamdata RENAME TO stream_ids;
