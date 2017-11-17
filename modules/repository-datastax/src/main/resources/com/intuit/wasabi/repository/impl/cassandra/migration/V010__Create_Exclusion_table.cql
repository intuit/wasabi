-- Create exclusions table to store metadata for mutual exclusion information

CREATE TABLE IF NOT EXISTS exclusion (
  base uuid,
  pair uuid,
  PRIMARY KEY (base, pair)
)WITH COMPACT STORAGE;
