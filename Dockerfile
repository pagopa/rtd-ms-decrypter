FROM public.ecr.aws/docker/library/maven:3.9.6-amazoncorretto-21@sha256:16dbd3a488a582cff1e42489f67b2b10b466e8a8eb1bdc4a1223d4e949812593 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre@sha256:493c1b23a728db105ac2c09c5af425421c409f16aed427c0be04f086825978b2 AS runtime


WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
RUN chown -R nobody:nogroup /app

USER 65534

ENTRYPOINT ["java","-jar","/app/app.jar"]
