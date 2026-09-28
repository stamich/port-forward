FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN useradd --system --uid 10001 --create-home portforward

COPY target/scala-2.13/port-forward-server-0.2.0.jar /app/port-forward-server.jar

USER 10001

ENTRYPOINT ["java", "-jar", "/app/port-forward-server.jar", "--cli"]
