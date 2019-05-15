FROM openjdk:11-jre-slim
MAINTAINER Gerard Klijs <gerard@openweb.nl>

ADD target/hb-docker.jar /app.jar

CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app.jar"]