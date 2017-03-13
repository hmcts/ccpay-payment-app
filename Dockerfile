FROM openjdk:8-jre

RUN curl -L https://raw.github.com/vishnubob/wait-for-it/master/wait-for-it.sh -o /usr/local/bin/wait-for-it.sh \
    && chmod +x /usr/local/bin/wait-for-it.sh

COPY docker/entrypoint.sh /

EXPOSE 8080

COPY api/target/payment-api-*.jar /app.jar

ENTRYPOINT [ "/entrypoint.sh" ]
