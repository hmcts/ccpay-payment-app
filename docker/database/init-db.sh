#!/usr/bin/env bash

set -e

# Claim Store database
if [ -z "SPRING_DATASOURCE_PASSWORD" ]; then
  echo "ERROR: Missing environment variables. Set value for 'SPRING_DATASOURCE_PASSWORD'."
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username postgres --set USERNAME=${SPRING_DATASOURCE_USERNAME} --set PASSWORD=${SPRING_DATASOURCE_PASSWORD} <<-EOSQL
  CREATE USER :USERNAME WITH PASSWORD ':PASSWORD';
  CREATE DATABASE payment
    WITH OWNER = :USERNAME
    ENCODING = 'UTF-8'
    CONNECTION LIMIT = -1;
EOSQL
