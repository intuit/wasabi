-- Query: Get the list of experiments to which an app/user has been assigned
--
--create table user_experiment_index (
--    app_name varchar,
--    user_id varchar,
--    experiment_id uuid,
--
--    -- Column value
--    bucket_label varchar,
--    PRIMARY KEY (app_name, user_id, experiment_id)
--)
--with compact storage;

create table user_experiment_index (
    app_name varchar,
    user_id varchar,
    context varchar,
    experiment_id uuid,

    -- Column value
    bucket_label varchar,
    PRIMARY KEY (app_name, user_id, context, experiment_id)
)
with compact storage;

