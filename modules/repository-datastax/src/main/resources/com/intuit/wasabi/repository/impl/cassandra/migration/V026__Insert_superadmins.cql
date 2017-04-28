---- Changes: add the admin user as the default superadmin

insert into user_roles (user_id, app_name, role) VALUES ('admin','*','superadmin');
insert into app_roles (app_name, user_id, role) VALUES ( '*','admin','superadmin');
INSERT INTO superadmins (user_id) VALUES ('admin');
