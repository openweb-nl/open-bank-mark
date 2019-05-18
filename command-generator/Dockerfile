FROM azul/zulu-openjdk-alpine:11-jre
MAINTAINER Gerard Klijs <gerard@openweb.nl>

ADD target/cg-docker.jar /app.jar

CMD ["java", "-jar", "/app.jar"]