CREATE TABLE `event_impression` (
  `user_id` varchar(48) COLLATE utf8_bin NOT NULL,
  `experiment_id` varbinary(16) NOT NULL,
  `bucket_label` varchar(64) NOT NULL,
  `timestamp` datetime NOT NULL,
  KEY `user_id` (`user_id`),
  KEY `experiment_id` (`experiment_id`),
  KEY `bucket_label` (`bucket_label`),
  KEY `timestamp` (`timestamp`),
  CONSTRAINT `event_impression_ibfk_1` FOREIGN KEY (`experiment_id`,`bucket_label`) REFERENCES `bucket` (`experiment_id`,`label`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
