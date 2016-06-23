CREATE TABLE experiment_audit_log (
  experiment_id uuid,
  modified timestamp,
  attribute_name text,
  old_value text,
  new_value text,
  PRIMARY KEY (experiment_id, modified, attribute_name)
);
