# renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.4.14
FROM hmctspublic.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/payment-app.jar /opt/app/

EXPOSE 8080

CMD [ \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "payment-app.jar" \
    ]
