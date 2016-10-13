-- Role: payment

-- DROP ROLE payment;

CREATE ROLE payment LOGIN
  ENCRYPTED PASSWORD 'md5d8a5ca10bc9f5d2f7dead1f60bd7d35b'
  NOSUPERUSER INHERIT NOCREATEDB NOCREATEROLE NOREPLICATION;

-- Database: payment

-- DROP DATABASE payment;

CREATE DATABASE payment
  WITH OWNER = payment
       ENCODING = 'UTF8'
       TABLESPACE = pg_default
       LC_COLLATE = 'English_United Kingdom.1252'
       LC_CTYPE = 'English_United Kingdom.1252'
       CONNECTION LIMIT = -1;

-- connect to the db

\c payment

-- Schema: payment

-- DROP SCHEMA payment;

CREATE SCHEMA payment
  AUTHORIZATION payment;



