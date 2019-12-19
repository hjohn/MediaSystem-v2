ALTER TABLE streams ADD COLUMN creation_ms int8 NOT NULL DEFAULT extract(epoch from now());

UPDATE streams SET creation_ms = sids.modtime FROM (SELECT id, modtime FROM stream_ids) AS sids WHERE sids.id = stream_id;
  
ALTER TABLE streams ALTER COLUMN creation_ms DROP DEFAULT;