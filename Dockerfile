FROM ghcr.io/navikt/sif-baseimages/java-25:2026.10.12.1155Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-tilgangsstyring

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

USER nonroot

CMD [ "-jar", "app.jar" ]
