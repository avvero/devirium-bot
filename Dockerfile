####
# Build image
####
FROM amazoncorretto:21-alpine3.21-jdk AS build
LABEL maintainer=avvero

RUN apk add --no-cache findutils

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
FROM amazoncorretto:21-alpine3.21-jdk

COPY --from=build /app/build/install/devirium-bot-boot devirium-bot-boot

EXPOSE 8080

CMD ["./devirium-bot-boot/bin/devirium-bot"]