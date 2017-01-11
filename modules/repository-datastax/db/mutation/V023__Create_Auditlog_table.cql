-- Note: the INSERT query works only with cassandra 2.0.7+!
--
-- Queries
--===========
-- SELECT * FROM auditlog WHERE application_name = '' [AND time ...];
--     PrimaryKey: (application_name), time
--
-- INSERT INTO auditlog (event_id, time, action, user_firstname, user_lastname, user_email, user_username,
--                       application_name, experiment_id, experiment_label, bucket_label, changed_property,
--                       property_before, property_after)
--               VALUES (uuid(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
--
-- 'null' values for primary keys:
-- application_name must not be null but will be null for actions not affecting any applications. In that case we use
-- a special value instead of null:
--     dGhpcyBpcyBhIG51bGwgZmllbGQ
--

-- 'original' AuditLog table
CREATE TABLE auditlog (
    time             timestamp,
    event_id         uuid,
    action           varchar,
    user_firstname   varchar,
    user_lastname    varchar,
    user_email       varchar,
    user_username    varchar,
    user_userid      varchar,
    application_name varchar,
    experiment_id    uuid,
    experiment_label varchar,
    bucket_label     varchar,
    changed_property varchar,
    property_before  varchar,
    property_after   varchar,
    PRIMARY KEY (
        (application_name),
        time,
        event_id
    )
)
WITH
    CLUSTERING ORDER BY (
        time DESC,
        event_id ASC
    )
AND
    COMMENT = 'Holds AuditLogEntries partitioned by application_name.';
