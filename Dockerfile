FROM maven:3.9.5-amazoncorretto-17-al2023@sha256:b7f94a5f1b6582a045692e31c2c97ef6f0ed867961669a0adbc2d5f0bbf8bc85 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:17.0.9-alpine3.18@sha256:5c009904e51559c23b3a026c1c93c14d3abfb94ed140207e7e694d3e2362dd0a AS runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar

RUN chown -R nobody:nobody /app

USER 65534

ENTRYPOINT ["java","-jar","/app/app.jar"]
