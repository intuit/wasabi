ALTER TABLE user_experiment_properties MODIFY user_id varchar(200) COLLATE utf8_bin NOT NULL;
ALTER TABLE event_impression MODIFY user_id varchar(200) COLLATE utf8_bin NOT NULL;
ALTER TABLE event_action MODIFY user_id varchar(200) COLLATE utf8_bin NOT NULL;
