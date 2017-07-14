CREATE TABLE `experiment_rollup` (
  `experiment_id` varbinary(16) NOT NULL,
  `day` date NOT NULL,
  `cumulative` boolean NOT NULL,
  `bucket_label` varchar(64) NOT NULL,
  `action` varchar(64) COLLATE utf8_bin NOT NULL DEFAULT '',
  `impression_count` integer DEFAULT NULL,
  `impression_user_count` integer DEFAULT NULL,
  `action_count` integer DEFAULT NULL,
  `action_user_count` integer DEFAULT NULL,
  UNIQUE KEY `entry` (`experiment_id`,`day`,`cumulative`,`bucket_label`,`action`),
  CONSTRAINT `experiment_rollup_ibfk_1` FOREIGN KEY (`experiment_id`,`bucket_label`) REFERENCES `bucket` (`experiment_id`,`label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
