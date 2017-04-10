CREATE TABLE bucket_audit_log (
  experiment_id uuid,
  label text,
  modified timestamp,
  attribute_name text,
  old_value text,
  new_value text,
  PRIMARY KEY ((experiment_id, label), modified, attribute_name)
);
