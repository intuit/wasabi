CREATE TABLE page_experiment_index (
  app_name text,
  page text,
  exp_id uuid,
  assign boolean,
  PRIMARY KEY ((app_name, page), exp_id)
);