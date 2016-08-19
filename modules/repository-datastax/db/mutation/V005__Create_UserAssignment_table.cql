---- Query: Get the assignment of a user to an experiment
--THIS IS COMMENTED OUT BECAUSE WE ARE USING A NEWER VERSION
--create table user_assignment (
--    experiment_id uuid,
--    user_id varchar,
--
--    bucket_label varchar,
--    created timestamp,
--    -- Use a composite partition key to avoid hotspotting
--    PRIMARY KEY ((experiment_id, user_id))
--);

create table user_assignment (
    experiment_id uuid,
    user_id varchar,
    context varchar,
    bucket_label varchar,
    created timestamp,
    PRIMARY KEY (experiment_id, context, user_id)
);