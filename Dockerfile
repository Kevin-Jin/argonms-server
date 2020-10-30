FROM openjdk:8

RUN apt-get update && \
    apt-get install -y maven

WORKDIR /app
ADD . /app

RUN mvn package
RUN cp target/argonms-*.jar dist/argonms.jar
