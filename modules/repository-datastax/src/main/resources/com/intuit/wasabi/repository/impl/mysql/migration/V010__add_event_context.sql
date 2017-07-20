alter table event_impression add context varchar(200) DEFAULT "PROD" COLLATE utf8_bin;
alter table event_action add context varchar(200) DEFAULT "PROD" COLLATE utf8_bin;
