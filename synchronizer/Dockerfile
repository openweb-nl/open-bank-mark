FROM azul/zulu-openjdk-alpine:11-jre
MAINTAINER Gerard Klijs <gerard@openweb.nl>

ADD target/syn-docker.jar /app.jar

CMD ["java", "-jar", "/app.jar"]