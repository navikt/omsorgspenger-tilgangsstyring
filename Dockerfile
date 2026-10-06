FROM ghcr.io/navikt/sif-baseimages/java-25:2026.10.05.1242Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-tilgangsstyring

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

USER nonroot

CMD [ "-jar", "app.jar" ]
