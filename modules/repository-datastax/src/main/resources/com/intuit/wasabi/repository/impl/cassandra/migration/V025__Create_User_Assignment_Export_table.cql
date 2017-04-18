create table user_assignment_export(
  experiment_id uuid,
  user_id varchar,
  context varchar,
  bucket_label varchar,
  is_bucket_null boolean,
  created timestamp,
  day_hour timestamp,
  PRIMARY KEY ((experiment_id, day_hour), context, is_bucket_null, user_id)
);