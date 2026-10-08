FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG DOCKER_USER
ARG DOCKER_GROUP

ENV APP_USER=${DOCKER_USER:-springuser}
ENV APP_GROUP=${DOCKER_GROUP:-springgroup}

RUN addgroup -S $APP_GROUP && adduser -S $APP_USER -G $APP_GROUP

COPY --from=build /app/target/gas-station-backend-0.0.1-SNAPSHOT.jar app.jar
RUN chown $APP_USER:$APP_GROUP app.jar

USER $APP_USER

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
