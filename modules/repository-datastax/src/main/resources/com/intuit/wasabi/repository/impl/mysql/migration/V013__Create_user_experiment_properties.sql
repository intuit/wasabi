CREATE TABLE IF NOT EXISTS `user_experiment_properties` (
  `user_id` varchar(48) COLLATE utf8_bin NOT NULL,
  `experiment_id` varbinary(16) NOT NULL,
  `is_favorite` BIT DEFAULT 0,
  PRIMARY KEY (`user_id`, `experiment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
