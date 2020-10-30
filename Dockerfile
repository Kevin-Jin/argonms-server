FROM openjdk:8

RUN apt-get update && \
    apt-get install -y maven

WORKDIR /app
COPY pom.xml pom.xml
RUN mvn dependency:go-offline
ADD . /app
RUN mvn package
