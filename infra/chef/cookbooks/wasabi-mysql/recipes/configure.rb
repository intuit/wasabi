# Configure Database and User

# Download the create_db.sql file to chef cache 
template "#{Chef::Config[:file_cache_path]}/create_db.sql" do
  source 'create_db.sql.erb'
  owner "root"
  group "root"
  mode 0755
  notifies :run, 'execute[create_db_user]', :immediately
end

# Run the create_db.sql script as root, which creates the jabba database and readwrite user and grants it all privileges on jabba database
execute "create_db_user" do
  command "mysql -h127.0.0.1 -u root -ppassword < #{Chef::Config[:file_cache_path]}/create_db.sql"
  action :nothing
end