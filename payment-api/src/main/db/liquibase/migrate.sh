#!/bin/bash

java -jar liquibase.jar --classpath=postgresql-9.2-1002-jdbc4.jar --changeLogFile=payment_migrations.xml --username=payment --password=payment --url="jdbc:postgresql://localhost:5432/payment" --driver=org.postgresql.Driver --defaultSchemaName=payment --logLevel=info  update
