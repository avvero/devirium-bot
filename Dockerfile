####
# Build image
####
FROM eclipse-temurin:21-jdk-jammy AS build
LABEL maintainer=avvero

RUN apt-get update && apt-get install -y findutils && rm -rf /var/lib/apt/lists/*

COPY gradlew /app/
COPY gradle /app/gradle
WORKDIR /app
RUN ./gradlew --version

WORKDIR /app
COPY . .
RUN ./gradlew installBootDist --no-daemon

####
# Runtime image
####
FROM eclipse-temurin:21-jdk-jammy

RUN groupadd --system appgroup && useradd --system -g appgroup appuser

COPY --from=build /app/build/install/devirium-bot-boot devirium-bot-boot

RUN chown -R appuser:appgroup /devirium-bot-boot

USER appuser

EXPOSE 8080

CMD ["./devirium-bot-boot/bin/devirium-bot"]