alter table event_impression modify context varchar(200) DEFAULT "PROD" COLLATE utf8_general_ci;
alter table event_action modify context varchar(200) DEFAULT "PROD" COLLATE utf8_general_ci;
alter table experiment_rollup modify context varchar(200) DEFAULT "PROD" COLLATE utf8_general_ci;
