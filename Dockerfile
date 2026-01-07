FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends \
    libreoffice-writer \
    libreoffice-core \
    fonts-dejavu \
    fonts-liberation \
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /src/target/*.jar /app/app.jar

ENV SOFFICE_PATH=/usr/bin/soffice
ENV HOME=/tmp

ENTRYPOINT ["java","-jar","/app/app.jar"]
