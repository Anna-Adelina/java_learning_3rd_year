# Build context = repository root:
#   docker build -f book-service/Dockerfile -t book-service .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# poms first (the root pom lists all modules, so all of them must be present)
COPY pom.xml .
COPY common/pom.xml common/
COPY book-service/pom.xml book-service/
COPY reporting-service/pom.xml reporting-service/
# sources: only this service and common (the only shared module)
COPY common/src common/src
COPY book-service/src book-service/src
RUN mvn -B -q -pl book-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/book-service/target/book-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
