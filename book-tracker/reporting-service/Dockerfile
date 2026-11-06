# Build context = repository root:
#   docker build -f reporting-service/Dockerfile -t reporting-service .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# poms first (the root pom lists all modules, so all of them must be present)
COPY pom.xml .
COPY common/pom.xml common/
COPY book-service/pom.xml book-service/
COPY reporting-service/pom.xml reporting-service/
# sources: only this service and common (the only shared module)
COPY common/src common/src
COPY reporting-service/src reporting-service/src
RUN mvn -B -q -pl reporting-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/reporting-service/target/reporting-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
