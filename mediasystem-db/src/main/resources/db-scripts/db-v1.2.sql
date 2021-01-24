
# Convert Duration of 0 to null in metadata:
UPDATE stream_metadata SET json = (convert_from(json, 'UTF8')::jsonb || jsonb '{"duration": null}')::text::bytea
  WHERE convert_from(json, 'UTF8')::json ->> 'duration' = 'PT0S';
