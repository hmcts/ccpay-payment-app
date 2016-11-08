################################################################################################
To run application on sanbox use following command.

java  -jar target\payment-api-0.0.1-SNAPSHOT.jar --spring.config.location=/etc/payment-api/

where
/etc/payment-api/ is the location of application.properties


################################################################################################
To run acceptance test cases use following command
mvn clean install -Pacceptance
or
mvn clean install -Denv="acceptance"

################################################################################################

To run acceptance test cases and generate report use following command
mvn clean install -Denv="acceptance" surefire-report:failsafe-report-only

