-- Query: Get bucket metadata

create table if not exists bucket (
    experiment_id uuid,
    label varchar,
    state text,
    description varchar,
    allocation double,
    is_control boolean,
    payload varchar,
    PRIMARY KEY (experiment_id, label)
);
