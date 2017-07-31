CREATE TABLE `experiment` (
  `id` varbinary(16) NOT NULL,
  `version` integer NOT NULL DEFAULT 0,
  `creation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modification_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `description` varchar(256) COLLATE utf8_bin NOT NULL DEFAULT '',
  `sampling_percent` double NOT NULL,
  `start_time` datetime NOT NULL,
  `end_time` datetime NOT NULL,
  `state` varchar(16) COLLATE utf8_bin NOT NULL DEFAULT 'DRAFT',
  `label` varchar(64) COLLATE utf8_bin NOT NULL,
  `app_name` varchar(64) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `experiment_unique` (`app_name`,`label`,`version`),
  KEY `start_time` (`start_time`),
  KEY `end_time` (`end_time`),
  KEY `state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
