alter table experiment_rollup
add context varchar(200) NOT NULL DEFAULT 'PROD',
DROP INDEX entry,
ADD UNIQUE KEY entry (experiment_id, day, cumulative, bucket_label, action, context);
