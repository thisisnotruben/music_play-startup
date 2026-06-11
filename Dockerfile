FROM eclipse-temurin:25-jre

ARG JAR_PATH=target/
ARG JAR_NAME=Startup-0.0.1-SNAPSHOT

COPY ${JAR_PATH}${JAR_NAME}.jar /opt/app.jar

COPY data/audioData.tar /etc/audioData.tar
COPY data/data.json /etc/data.json
COPY data/data.tar /etc/data.tar
COPY data/music-play-config.zip /etc/music-play-config.zip

RUN apt-get update && apt-get install -y curl

EXPOSE 8080
ENTRYPOINT java -jar /opt/app.jar
