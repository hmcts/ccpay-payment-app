#!/usr/bin/env bash

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE USER payment;
    CREATE DATABASE payment
        WITH OWNER = payment
        ENCODING ='UTF-8'
        CONNECTION LIMIT = -1;
EOSQL

psql -v ON_ERROR_STOP=1 --dbname=payment --username "$POSTGRES_USER" <<-EOSQL
    CREATE SCHEMA payment AUTHORIZATION payment;
EOSQL
