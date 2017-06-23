CREATE TABLE `bucket` (
  `experiment_id` varbinary(16) NOT NULL,
  `label` varchar(64) COLLATE utf8_bin NOT NULL,
  `allocation_percent` double NOT NULL,
  `is_control` tinyint(1) NOT NULL DEFAULT '0',
  `payload` varchar(4096) COLLATE utf8_bin NOT NULL DEFAULT '',
  `description` varchar(256) COLLATE utf8_bin NOT NULL DEFAULT '',
  PRIMARY KEY (`experiment_id`,`label`),
  KEY `experiment_id` (`experiment_id`),
  CONSTRAINT `buckets_ibfk_1` FOREIGN KEY (`experiment_id`) REFERENCES `experiment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
