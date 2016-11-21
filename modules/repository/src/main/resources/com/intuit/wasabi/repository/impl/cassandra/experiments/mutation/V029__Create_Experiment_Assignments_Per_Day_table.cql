-- Queries
--===========
-- To select the data:
-- SELECT bucket_assignment FROM experiment_assignment_type WHERE experiment_id = ? AND timestamp >= ? AND timestamp < ? ;
--     PrimaryKey: ((experiment_id), timestamp)
--
-- To insert data:
-- INSERT INTO experiment_assignment_type ( experiment_id, timestamp, bucket_assignment ) VALUES ( ?, ?, ? );
--
-- 'original' experiment_assignment_type table

CREATE TABLE experiment_assignment_type (
    experiment_id uuid,
    timestamp timestamp,
    bucket_assignment boolean,
    PRIMARY KEY ((experiment_id), timestamp)
) WITH CLUSTERING ORDER BY (timestamp DESC)
  AND COMMENT = 'If bucket_assignment is true, a user was assigned to a bucket. If it is false, a user was assigned null.';
