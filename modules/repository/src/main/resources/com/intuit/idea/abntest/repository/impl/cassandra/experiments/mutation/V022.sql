CREATE TABLE bucket_assignment_counts
  (experiment_id uuid,
   bucket_label varchar,
   bucket_assignment_count counter,
  PRIMARY KEY (experiment_id, bucket_label)
);

