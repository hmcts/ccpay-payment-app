ARG APP_INSIGHTS_AGENT_VERSION=2.5.1
FROM hmctspublic.azurecr.io/base/java:11-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/payment-app.jar /opt/app/

EXPOSE 8080

CMD ["payment-app.jar"]
