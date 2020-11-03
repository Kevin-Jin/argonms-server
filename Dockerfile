FROM openjdk:8

RUN apt-get update && \
    apt-get install -y maven default-mysql-client

ENV HOME=/app
WORKDIR /app
COPY pom.xml pom.xml
RUN mvn dependency:go-offline
ADD . /app
RUN mvn compile package
