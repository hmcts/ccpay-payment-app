FROM hmcts/cnp-java-base:openjdk-jre-8-alpine-1.4

ENV APP payment-app.jar
ENV APPLICATION_TOTAL_MEMORY 512M
ENV APPLICATION_SIZE_ON_DISK_IN_MB 128

COPY docker/entrypoint.sh /

EXPOSE 8080

# Optional
ENV JAVA_OPTS ""

COPY build/libs/$APP /app.jar

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy= curl --silent --fail http://localhost:8080/health

ENTRYPOINT [ "/entrypoint.sh" ]
