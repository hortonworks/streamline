CREATE USER 'streamline_user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'streamline_user'@'%';
CREATE DATABASE streamline_db;
CREATE USER 'registry_user'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'registry_user'@'%';
CREATE DATABASE schema_registry;
CREATE database druid CHARACTER SET utf8;
CREATE USER 'druid'@'%' IDENTIFIED BY 'druid';
GRANT ALL ON *.* TO 'druid'@'%' IDENTIFIED BY 'druid';
