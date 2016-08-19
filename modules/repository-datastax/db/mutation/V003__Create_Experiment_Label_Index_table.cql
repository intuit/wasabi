-- Query: Get the experiment associated with an app/label locator

create table experiment_label_index (
    app_name varchar,
    label varchar,

    id uuid,
    modified timestamp,

    -- Cache these properties
    start_time timestamp,
    end_time timestamp,
    state varchar,

    -- Use a composite partition key to avoid hotspotting
    PRIMARY KEY ((app_name, label))
);

-- Query: Get the experiment labels associated with an app
create index on experiment_label_index(app_name);