FROM azul/zulu-openjdk-alpine:11-jre
MAINTAINER Gerard Klijs <gerard@openweb.nl>

ADD target/hb-docker.jar /app.jar

CMD ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app.jar"]