-- Create exclusions table to store metadata for mutual exclusion information

CREATE TABLE exclusion (
  base uuid,
  pair uuid,
  PRIMARY KEY (base, pair)
)WITH COMPACT STORAGE;