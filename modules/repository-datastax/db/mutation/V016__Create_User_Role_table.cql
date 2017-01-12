CREATE TABLE user_roles (
  user_id text,
  app_name text,
  role text,
  PRIMARY KEY (user_id, app_name)
);

INSERT INTO user_roles (user_id, app_name, role) VALUES ( 'admin', 'admin', 'superadmin');
