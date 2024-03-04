FROM maven:3.9.5-amazoncorretto-17-al2023@sha256:b7f94a5f1b6582a045692e31c2c97ef6f0ed867961669a0adbc2d5f0bbf8bc85 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM amazoncorretto:17.0.10-alpine3.19@sha256:7fa638463726cb1646100882b2baafd1b78978007bbfc28cb36a7fb5e9ebe8dd AS runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar

RUN chown -R nobody:nobody /app

USER 65534

ENTRYPOINT ["java","-jar","/app/app.jar"]
