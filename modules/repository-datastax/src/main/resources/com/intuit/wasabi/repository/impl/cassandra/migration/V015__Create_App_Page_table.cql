CREATE TABLE IF NOT EXISTS app_page_index (
  app_name text,
  page text,
  PRIMARY KEY (app_name, page)
) WITH COMPACT STORAGE;
