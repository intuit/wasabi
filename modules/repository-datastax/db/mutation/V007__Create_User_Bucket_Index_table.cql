-- Query: Get the list of users assigned to a bucket
--commented out for migration to context flg

--create table user_bucket_index (
--    experiment_id uuid,
--    bucket_label varchar,
--    user_id varchar,
--
--    -- Column value
--    assigned timestamp,
--    PRIMARY KEY ((experiment_id, bucket_label), user_id)
--)
--with compact storage;

create table user_bucket_index (
    experiment_id uuid,
    bucket_label varchar,
    user_id varchar,
    context varchar,
    -- Column value
    assigned timestamp,
    PRIMARY KEY ((experiment_id, context, bucket_label), user_id)
)
with compact storage;