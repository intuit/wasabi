-- Create priority table to store priority list for priority api

CREATE TABLE IF NOT EXISTS application (

  app_name varchar,
  priorities list<uuid>,
  PRIMARY KEY (app_name)
);
