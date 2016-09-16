-- Queries
--===========
-- To select the data:
-- SELECT * FROM experiment_assignments_per_day WHERE experiment_id = ? AND context = ? AND day >= ? AND day <= ? ;
--     PrimaryKey: ((experiment_id), day)
--
-- To update counts for either a bucket assignment or a null assignment:
-- UPDATE experiment_assignments_per_day SET bucket_assignments + 1 WHERE experiment_id = ? AND context = ? AND day = ? ;
-- UPDATE experiment_assignments_per_day SET null_assignments + 1 WHERE experiment_id = ? AND context = ? AND day = ? ;
--

-- 'original' experiment_assignments_per_day table
CREATE TABLE experiment_assignments_per_day (
    experiment_id uuid,
    day varchar,
    context varchar,
    bucket_assignments counter,
    null_assignments counter,
    PRIMARY KEY ((experiment_id, context), day)
) WITH CLUSTERING ORDER BY (day ASC)
  AND COMMENT = 'Holds counters to count total new bucket assignments and new null assignments per experiment per day.';
