# Build- und Laufumgebung fuer den Legacy-Stand (Java 8).
# Der Container ist Projekt-Infrastruktur (Reproduzierbarkeit), nicht Teil
# des 2016-Exhibits - siehe LEGACY_NOTES.md.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/werkstatt-crm.war /app/werkstatt-crm.war
EXPOSE 8080
# JSP funktioniert im ausfuehrbaren WAR mit eingebettetem Tomcat
ENTRYPOINT ["java", "-jar", "/app/werkstatt-crm.war"]
