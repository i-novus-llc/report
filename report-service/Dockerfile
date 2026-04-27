FROM images.digitalms.ru/library/redos7c/jdk-17:17.0.13-1.30.0

USER root

RUN dnf install -y dejavu-serif-fonts \
    && dnf clean all

USER 10001

EXPOSE 8080

ARG JAR_FILE
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","app.jar"]
