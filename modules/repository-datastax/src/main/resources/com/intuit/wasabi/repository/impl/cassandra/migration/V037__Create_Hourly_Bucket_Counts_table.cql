CREATE TABLE hourly_bucket_counts (
experiment_id uuid,
day text,
event_time_hour int,
bucket_label varchar,
hourly_bucket_count counter,
PRIMARY KEY ((experiment_id, day), event_time_hour, bucket_label)
);

