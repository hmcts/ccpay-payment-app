ARG APP_INSIGHTS_AGENT_VERSION=2.3.1
FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.0

EXPOSE 8080

COPY build/libs/payment-app.jar /opt/app/
COPY lib/applicationinsights-agent-2.3.1.jar lib/AI-Agent.xml /opt/app/
COPY --chown=root ./payment-audit.log /opt/app

CMD ["payment-app.jar"]
