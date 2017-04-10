CREATE TABLE experiment_page (
  exp_id uuid,
  page text,
  assign boolean,
  PRIMARY KEY (exp_id, page)
) WITH COMPACT STORAGE;