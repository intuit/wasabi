-- Query: Get bucket metadata

create table bucket (
    experiment_id uuid,
    label varchar,
    state text,
    description varchar,
    allocation double,
    is_control boolean,
    payload varchar,
    PRIMARY KEY (experiment_id, label)
);
