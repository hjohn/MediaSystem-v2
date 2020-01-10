ALTER TABLE streamdata DROP CONSTRAINT streamdata_url;
ALTER TABLE streamdata ADD CONSTRAINT streamdata_url UNIQUE (url);
