create table experiment_tag (
  app_name text,
  tags set<text>,
  PRIMARY KEY (app_name)
);