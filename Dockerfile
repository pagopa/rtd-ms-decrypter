FROM maven:3.9.7-amazoncorretto-21-al2023@sha256:7ab0e813ffe398b2fa97d34230b7c423d92f9c35b06d96b8bb6f60aecf2be98a AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:21.0.3-alpine3.19@sha256:7e522a694566c0c6cd80b06d97bc69f4be31a518d81d6cdd30c9a854a56aa84a AS runtime

WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar

RUN chown -R nobody:nobody /app

USER 65534

ENTRYPOINT ["java","-jar","/app/app.jar"]
