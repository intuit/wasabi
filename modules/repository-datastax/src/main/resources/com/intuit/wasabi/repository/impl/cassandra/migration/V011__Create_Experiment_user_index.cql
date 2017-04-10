---- Query: Get the list of experiments to which an app/user has been assigned
--
--create table experiment_user_index (
--    user_id varchar,
--    app_name varchar,
--    bucket varchar,
--    experiment_id uuid,
--
--    -- Column value
--    PRIMARY KEY (user_id, app_name, experiment_id)
--)
--;

create table experiment_user_index (
    user_id varchar,
    context varchar,
    app_name varchar,
    bucket varchar,
    experiment_id uuid,

    -- Column value
    PRIMARY KEY (user_id, context, app_name, experiment_id)
)
;
