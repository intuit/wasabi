create table user_assignment_look_up(
  experiment_id uuid,
  user_id varchar,
  context varchar,
  bucket_label varchar,
  created timestamp,
  PRIMARY KEY (user_id, context, experiment_id)
);
