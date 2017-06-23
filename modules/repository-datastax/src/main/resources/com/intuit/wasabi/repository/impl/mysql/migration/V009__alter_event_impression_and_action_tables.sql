alter table event_impression add payload varchar(4096) COLLATE utf8_bin;
alter table event_action add payload varchar(4096) COLLATE utf8_bin;
