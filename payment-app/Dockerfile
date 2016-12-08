FROM openjdk:8-jre

EXPOSE 8181

ENV SPRING_DATASOURCE_URL "jdbc:postgresql://payments-postgres:5432/payment"
COPY api/target/payment-api-*.jar /app.jar

ENTRYPOINT [ "java",  "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar" ]
