create table experiment_tag (
  app_name text,
  exp_id uuid,
  tags set<text>,
  PRIMARY KEY (app_name, exp_id)
);