FROM openjdk:11.0.5-slim AS builder
WORKDIR /app

COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN ./mvnw dependency:go-offline
COPY . .
RUN ./mvnw -B -X package


FROM openjdk:11.0.5-jre-slim
MAINTAINER maciej.wojtaczka

RUN groupadd -g 999 appuser && useradd -r -u 999 -g appuser appuser
USER appuser
COPY --from=builder /app/target/timeline-*.jar opt/timeline/timeline.jar

ENTRYPOINT ["java", "-jar", "opt/timeline/timeline.jar"]
EXPOSE 8080
