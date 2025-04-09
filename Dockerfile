FROM public.ecr.aws/docker/library/maven:3.9.6-amazoncorretto-17@sha256:b64f097a87e94f3fb433649f2a49270564fa626494d7d6bfd8955f32da794210 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre@sha256:493c1b23a728db105ac2c09c5af425421c409f16aed427c0be04f086825978b2 AS runtime


WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
RUN chown -R nobody:nobody /app

USER 65534

ENTRYPOINT ["java","-jar","/app/app.jar"]
