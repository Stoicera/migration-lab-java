# Build and runtime for the modern stand (Java 25, Spring Boot 4.1).
# Project infrastructure, grows stage by stage - see stages.md.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
# frontend first: its npm ci layer only busts when package/lockfile change
COPY frontend ./frontend
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/werkstatt-crm.war /app/werkstatt-crm.war
EXPOSE 8080
# JSP funktioniert im ausfuehrbaren WAR mit eingebettetem Tomcat
ENTRYPOINT ["java", "-jar", "/app/werkstatt-crm.war"]
