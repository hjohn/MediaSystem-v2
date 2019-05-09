
# Add persistent_identifier column to streamdata table, extracting initial value from json column:

ALTER TABLE streamdata ADD COLUMN persistent_identifier UUID;
UPDATE streamdata SET persistent_identifier = (convert_from(json, 'UTF-8')::json->>'persistentIdentifier')::uuid;
ALTER TABLE streamdata ALTER COLUMN persistent_identifier SET NOT NULL;
ALTER TABLE streamdata ADD CONSTRAINT streamdata_identifier UNIQUE (persistent_identifier);

# Remove constraints from streamstate table:

ALTER TABLE streamstate DROP CONSTRAINT streamstate_unique;
ALTER TABLE streamstate DROP CONSTRAINT streamstate_hash_fkey;

# Add persistent_identifier column to streamstate table, and make it refer to streamdata table:

ALTER TABLE streamstate ADD COLUMN streamdata_id UUID;
UPDATE streamstate ss SET streamdata_id = (SELECT persistent_identifier FROM streamdata sd WHERE sd.hash = ss.hash AND sd.size = ss.size AND sd.modtime = ss.modtime);
ALTER TABLE streamstate ALTER COLUMN streamdata_id SET NOT NULL;
ALTER TABLE streamstate ADD CONSTRAINT streamstate_unique PRIMARY KEY (streamdata_id);
ALTER TABLE streamstate ADD CONSTRAINT streamdata_id_fkey FOREIGN KEY (streamdata_id) REFERENCES streamdata(persistent_identifier) ON DELETE CASCADE; 

# Finally, drop redundant columns:
 
ALTER TABLE streamstate DROP COLUMN hash;
ALTER TABLE streamstate DROP COLUMN size;
ALTER TABLE streamstate DROP COLUMN modtime;
