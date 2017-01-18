-- Query: Get experiment metadata

create table experiment (
    id uuid,

    description varchar,
    sample_percent double,
    start_time timestamp,
    end_time timestamp,
    state varchar,
    app_name varchar,
    label varchar,
    created timestamp,
    modified timestamp,
    rule varchar,
    model_name text,
    model_version text,
    is_personalized boolean,
    is_rapid_experiment boolean,
    user_cap int,
    creatorid text,
    PRIMARY KEY (id)
);

-- Query: Get all experiment IDs for an app
create index on experiment(app_name);
