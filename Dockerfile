FROM navikt/java:15
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-tilgangsstyring
COPY build/libs/*.jar app.jar
