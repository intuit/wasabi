-- Query: Get the status of an experiment (deleted or not)

create table state_experiment_index (
    index_key varchar,
    experiment_id uuid,

    -- Column value
    value blob,
    PRIMARY KEY (index_key, experiment_id)
)
with compact storage;

