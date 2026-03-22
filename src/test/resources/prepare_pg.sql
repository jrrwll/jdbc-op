create user myuser with password 'mypassword';

create database testdb
with owner=myuser template=template0
    encoding='UTF8' connection limit = 100;

grant all privileges on database testdb to myuser;

-- \c testdb
create schema test;

alter schema test owner to myuser;

grant all privileges on all tables in schema public to myuser;
grant all privileges on all tables in schema test to myuser;

